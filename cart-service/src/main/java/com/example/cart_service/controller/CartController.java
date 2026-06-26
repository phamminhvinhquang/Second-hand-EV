package com.example.cart_service.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.cart_service.client.ProductServiceClient;
import com.example.cart_service.dto.ProductDetailDTO;
import com.example.cart_service.model.Cart;
import com.example.cart_service.repository.CartRepository;
import com.example.cart_service.service.CartReconciliationService;

import feign.FeignException;
import lombok.RequiredArgsConstructor;


//REST controller cho endpoint /api/carts.
//Controller này chịu trách nhiệm CRUD cơ bản cho cart, hành vi tiện lợi như
//- add-by-product: thêm cart bằng productId (gọi product-service để lấy thông tin)
// - reconcile endpoints: dọn dẹp các item "mồ côi" (sản phẩm đã bị xóa ở listing-service)
// Lưu ý bảo mật: một số endpoint yêu cầu header X-User-Id để biết user hiện tại.
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartRepository cartRepository;
    private final ProductServiceClient productClient;

    // Service dùng để chạy tác vụ dọn dẹp (reconciliation)
    @Autowired
    private CartReconciliationService reconciliationService;

    // URL base của listing-service (dùng để ghép link ảnh nếu listing-service trả về path relative)
    @Value("${product.service.url:http://localhost:8080}")
    private String productServiceUrl;

    // Trả danh sách cart. Nếu có X-User-Id: verify từng item bằng product-service,
    // xóa cart nếu product trả 404, giữ item nếu gọi product-service lỗi tạm thời.
    @GetMapping
    public ResponseEntity<List<Cart>> getAll(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId != null) {
            List<Cart> list = cartRepository.findByUserIdOrderByIdDesc(userId);

            // --- verify each cart item; if product missing -> delete it immediately ---
            List<Cart> remaining = new java.util.ArrayList<>();
            for (Cart c : list) {
                Long pid = c.getProductId();
                if (pid == null) {
                    // nếu productId null thì xóa luôn (dữ liệu lỗi)
                    try {
                        cartRepository.deleteById(c.getId());
                    } catch (Exception ex) {
                        log.warn("Failed to delete cart id {} with null productId: {}", c.getId(), ex.getMessage());
                    }
                    continue;
                }
                try {
                    productClient.getProductDetail(pid); // nếu không ném => tồn tại
                    remaining.add(c);
                } catch (Exception ex) {
                    if (ex instanceof FeignException && ((FeignException) ex).status() == 404) {
                        log.info("Product {} not found, deleting cart id={}", pid, c.getId());
                        try {
                            cartRepository.deleteById(c.getId());
                        } catch (Exception de) {
                            log.warn("Failed to delete cart id {}: {}", c.getId(), de.getMessage());
                        }
                    } else {
                        // Nếu gọi product-service lỗi (timeout...), để cho user vẫn thấy item (không xóa)
                        log.warn("Could not verify product {} for cart id {}: {}. Keeping item for now.", pid, c.getId(), ex.getMessage());
                        remaining.add(c);
                    }
                }
            }
            return ResponseEntity.ok(remaining);
        } else {
            List<Cart> list = cartRepository.findAll();
            return ResponseEntity.ok(list);
        }
    }

    // Thêm một cart row từ body. Nếu có X-User-Id thì gán vào cart.
    // không kiểm tra product tồn tại.
    @PostMapping("/add")
    public ResponseEntity<Cart> add(@RequestBody Cart cart,
                                    @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId != null) {
            cart.setUserId(userId);
        }
        Cart saved = cartRepository.save(cart);
        return ResponseEntity.ok(saved);
    }

    // Thêm cart bằng productId (frontend thường gọi).
    // Yêu cầu X-User-Id; kiểm tra trùng; gọi product-service lấy thông tin; map DTO -> Cart.
    // Trả 401/409/404/500 tương ứng khi cần.
    @PostMapping("/add-by-product/{productId}")
    public ResponseEntity<?> addByProduct(@PathVariable Long productId,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        log.info("POST /api/carts/add-by-product productId={} X-User-Id={}", productId, userId);

        if (userId == null) {
            log.warn("Missing X-User-Id header - rejecting add-by-product");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("X-User-Id header required (user must be logged in).");
        }

        if (cartRepository.existsByProductIdAndUserId(productId, userId)) {
            log.info("Product {} already in cart for user {}", productId, userId);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Product already in cart for this user");
        }

        ProductDetailDTO pd;
        try {
            pd = productClient.getProductDetail(productId);
        } catch (Exception ex) {
            log.error("Error calling product service for productId={}", productId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error calling product service: " + ex.getMessage());
        }

        if (pd == null || pd.getProductId() == null) {
            log.warn("Product not found from product-service: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        }

        String img = "/images/product.jpg"; // fallback
        if (pd.getImageUrls() != null && !pd.getImageUrls().isEmpty()) {
            String first = pd.getImageUrls().get(0);
            if (first != null && !first.trim().isEmpty()) {
                // Chỉ lưu đường dẫn tương đối (vd: "/uploads/file.jpg")
                // Hoặc URL tuyệt đối nếu nó đã là (vd: http://.../file.jpg)
                img = first.trim();
            }
        }

        Long resolvedSellerId = pd.resolveSellerId();

        Cart cart = Cart.builder()
                .productname(pd.getProductName())
                .imgurl(img)
                .price(pd.getPrice() != null ? pd.getPrice().doubleValue() : 0.0)
                .total(pd.getPrice() != null ? pd.getPrice().doubleValue() : 0.0)
                .productId(pd.getProductId())
                .yearOfManufacture(pd.getYearOfManufacture())
                .brand(pd.getBrand())
                .conditionName(pd.getEffectiveConditionName())
                .mileage(pd.getMileage())
                .sellerId(resolvedSellerId)
                .userId(userId)
                .build();

        Cart saved = cartRepository.save(cart);
        log.info("Saved cart id={} productId={} userId={} sellerId={}", saved.getId(), saved.getProductId(), saved.getUserId(), saved.getSellerId());
        return ResponseEntity.ok(saved);
    }

    // Xóa cart theo id (idempotent). Nếu row không tồn tại vẫn trả 204.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (cartRepository.existsById(id)) {
            cartRepository.deleteById(id);
        }
        return ResponseEntity.noContent().build();
    }

    
    // Trả tóm tắt cart theo id (productName, price, sellerId, userId).
    // Nếu không tìm thấy -> 404.
    @GetMapping("/{id}")
    public ResponseEntity<?> getCartById(@PathVariable Long id) {
        return cartRepository.findById(id)
                .map(cart -> ResponseEntity.ok(Map.of(
                        "productName", cart.getProductname(),
                        "price", cart.getPrice(),
                        "sellerId", cart.getSellerId(),
                        "userId", cart.getUserId()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    // Admin/manual: chạy dọn dẹp toàn bộ DB (runReconciliation) và trả số bản ghi đã xóa.
    @GetMapping("/admin/reconcile")
    public ResponseEntity<String> triggerReconciliation() {
        log.info("Kích hoạt tác vụ dọn dẹp thủ công...");
        int count = reconciliationService.runReconciliation();
        String message = "Dọn dẹp hoàn tất. Đã xóa " + count + " sản phẩm mồ côi.";
        log.info(message);
        return ResponseEntity.ok(message);
    }

    
    // Xóa tất cả cart rows có productId = {productId}.
    // Dùng khi product bị xóa hoặc nhận event delete.
    @DeleteMapping("/by-product/{productId}")
    public ResponseEntity<Void> deleteByProduct(@PathVariable Long productId) {
        int deleted = cartRepository.deleteByProductId(productId);
        log.info("Deleted {} cart rows for productId={} via HTTP call", deleted, productId);
        return ResponseEntity.noContent().build();
    }

    // Chỉ reconcile cho user hiện tại (frontend nên gọi trước khi fetch cart).
    // Yêu cầu X-User-Id; trả số bản ghi đã xóa cho user đó.
    @GetMapping("/reconcile-user")
    public ResponseEntity<String> reconcileUser(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("X-User-Id required");
        int count = reconciliationService.reconcileForUser(userId);
        String msg = "Reconciled for user " + userId + ". Deleted: " + count;
        log.info(msg);
        return ResponseEntity.ok(msg);
    }
}
