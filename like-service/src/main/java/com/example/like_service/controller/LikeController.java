package com.example.like_service.controller;

import com.example.like_service.client.ProductServiceClient;
import com.example.like_service.dto.ProductDetailDTO;
import com.example.like_service.model.Like;
import com.example.like_service.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {
    private static final Logger log = LoggerFactory.getLogger(LikeController.class);

    private final LikeRepository likeRepository;
    private final ProductServiceClient productClient;

    @Value("${product.service.url:http://localhost:8080}")
    private String productServiceUrl;

    /**
     * GET /api/likes
     * Nếu header X-User-Id có -> trả likes của user đó
     * Nếu không -> trả tất cả (fallback cho dev)
     */
    @GetMapping
    public ResponseEntity<List<Like>> getAll(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId != null) {
            List<Like> list = likeRepository.findByUserIdOrderByIdDesc(userId);
            return ResponseEntity.ok(list);
        } else {
            return ResponseEntity.ok(likeRepository.findAll());
        }
    }

    // Thêm 1 bản ghi (dùng Postman)
    @PostMapping("/add")
    public ResponseEntity<Like> add(@RequestBody Like like) {
        Like saved = likeRepository.save(like);
        return ResponseEntity.ok(saved);
    }

    /**
     * POST /api/likes/add-by-product/{productId}
     * Thêm sản phẩm vào "giỏ liked" dựa trên productId
     * BẮT BUỘC: header X-User-Id phải có (user đang đăng nhập)
     */
    @PostMapping("/add-by-product/{productId}")
    public ResponseEntity<?> addByProduct(@PathVariable Long productId,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("POST /api/likes/add-by-product productId={} X-User-Id={}", productId, userId);

        if (userId == null) {
            log.warn("Missing X-User-Id header - rejecting add-by-product");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("X-User-Id header required (user must be logged in).");
        }

        // Nếu đã like rồi cho user này -> trả 409
        if (likeRepository.existsByProductIdAndUserId(productId, userId)) {
            log.info("Product {} already liked by user {}", productId, userId);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Product already liked by this user");
        }

        // Gọi product-service lấy thông tin
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

        // Xử lý image giống pattern
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
        if (resolvedSellerId == null) {
            log.warn("Cannot resolve sellerId for productId={}. ProductDetailDTO may not contain seller info.", productId);
        } else {
            log.info("Resolved sellerId={} for productId={}", resolvedSellerId, productId);
        }

        Like like = Like.builder()
                .productId(pd.getProductId())
                .productname(pd.getProductName())
                .imgurl(img)
                .price(pd.getPrice() != null ? pd.getPrice().doubleValue() : 0.0)
                .yearOfManufacture(pd.getYearOfManufacture())
                .brand(pd.getBrand())
                .conditionName(pd.getEffectiveConditionName())
                .mileage(pd.getMileage())
                .sellerId(resolvedSellerId)
                .userId(userId)
                .build();

        Like saved = likeRepository.save(like);
        log.info("Saved like id={} productId={} userId={} sellerId={}", saved.getId(), saved.getProductId(), saved.getUserId(), saved.getSellerId());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        if (likeRepository.existsById(id)) {
            likeRepository.deleteById(id);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Xóa theo productId + userId (dùng khi toggle favorite từ trang chính)
     * Yêu cầu X-User-Id header
     */
    @DeleteMapping("/by-product/{productId}")
    public ResponseEntity<Void> deleteByProduct(@PathVariable Long productId,
                                                @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Like> opt = likeRepository.findByProductIdAndUserId(productId, userId);
        opt.ifPresent(like -> likeRepository.deleteById(like.getId()));
        return ResponseEntity.noContent().build();
    }
}
