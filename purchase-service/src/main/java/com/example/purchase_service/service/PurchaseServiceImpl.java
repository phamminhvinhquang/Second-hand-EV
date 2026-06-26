package com.example.purchase_service.service;

import com.example.purchase_service.client.ProductClient;
import com.example.purchase_service.dto.CreatePurchaseRequest;
import com.example.purchase_service.dto.PaymentInfoResponse;
import com.example.purchase_service.dto.ProductDetailDTO;
import com.example.purchase_service.model.Purchase;
import com.example.purchase_service.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map; // <-- Thêm import

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ProductClient productClient; // Feign client
    private final RestTemplate restTemplate;

    @Value("${transaction.service.url:http://localhost:8083}")
    private String transactionServiceUrl;

    @Override
    public Purchase createNewPurchase(Purchase purchase) {
        // (Giữ nguyên)
        if (purchase.getStatus() == null) {
            purchase.setStatus("waiting_delivery");
        }
        if (purchase.getCreatedAt() == null) {
            purchase.setCreatedAt(LocalDateTime.now());
        }
        return purchaseRepository.save(purchase);
    }

    /**
     * 🛑 HÀM ĐÃ SỬA LỖI: Xử lý tạo Purchase từ sự kiện MQ 'order.paid'
     * Hàm này kết hợp (1) Dữ liệu ID từ MQ và (2) Dữ liệu khách hàng từ REST.
     */
    @Override
    @Transactional
    public Purchase createPurchaseFromEvent(Map<String, Object> payload) {
        String transactionId = String.valueOf(payload.get("transactionId"));

        // 1. Kiểm tra trùng lặp
        if (existsByTransactionId(transactionId)) {
            log.warn("Purchase for transactionId={} already exists (from event).", transactionId);
            return purchaseRepository.findByTransactionId(transactionId).orElseThrow();
        }

        // === 🛑 LOGIC SỬA LỖI BẮT ĐẦU ===

        // 2. Lấy thông tin khách hàng (fullName, phone...) qua REST
        String url = transactionServiceUrl.endsWith("/") ?
                transactionServiceUrl + "api/payments/info/" + transactionId :
                transactionServiceUrl + "/api/payments/info/" + transactionId;

        log.info("Fetching CUSTOMER info from transaction-service (via MQ flow): {}", url);
        PaymentInfoResponse info;
        try {
            info = restTemplate.getForObject(url, PaymentInfoResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch customer info from transaction-service (url={}): {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch customer info from transaction-service: " + e.getMessage(), e);
        }
        if (info == null) {
            throw new RuntimeException("No payment info returned for transactionId: " + transactionId);
        }
        
        // 3. Map dữ liệu
        CreatePurchaseRequest req = new CreatePurchaseRequest();
        
        // 3a. Dữ liệu từ REST (Đáng tin cậy cho thông tin khách hàng)
        req.setTransactionId(transactionId);
        req.setFullName(info.getFullName());
        req.setPhone(info.getPhone());
        req.setEmail(info.getEmail());
        req.setAddress(info.getAddress());
        
        // 3b. Dữ liệu từ MQ Payload (Đáng tin cậy cho IDs và giá)
        req.setUserId(getLong(payload.get("userId")));
        req.setSellerId(getLong(payload.get("sellerId")));
        req.setProductId(getLong(payload.get("productId")));
        
        // Ưu tiên tên SP từ MQ, nếu không có mới lấy từ REST
        req.setProductName(String.valueOf(payload.getOrDefault("productName", info.getProductName()))); 
        
        if (payload.get("price") instanceof Number) {
            req.setPrice(((Number) payload.get("price")).doubleValue());
        } else {
            req.setPrice(info.getPrice() == 0.0 ? info.getTotalAmount() : info.getPrice()); // Fallback
        }

        // === 🛑 LOGIC SỬA LỖI KẾT THÚC ===

        // 4. Gọi hàm createPurchase chung
        // Bây giờ 'req' đã CÓ CẢ sellerId VÀ customer_full_name
        return createPurchase(req);
    }

    /**
     * Helper an toàn để chuyển đổi các kiểu Number (Integer, Long, Double) từ Map
     */
    private Long getLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }


    /**
     * HÀM CŨ (Fallback): Dùng cho API (từ payment_success.js)
     */
    @Override
    @Transactional
    public Purchase createPurchaseFromTransaction(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }

        if (existsByTransactionId(transactionId)) {
            log.warn("Purchase for transactionId={} already exists (from REST).", transactionId);
            return purchaseRepository.findByTransactionId(transactionId).orElseThrow();
        }

        String url = transactionServiceUrl.endsWith("/") ?
                transactionServiceUrl + "api/payments/info/" + transactionId :
                transactionServiceUrl + "/api/payments/info/" + transactionId;

        log.info("Fetching payment info from transaction-service (REST Fallback): {}", url);
        PaymentInfoResponse info;
        try {
            info = restTemplate.getForObject(url, PaymentInfoResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch payment info (REST Fallback) from transaction-service (url={}): {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch payment info from transaction-service: " + e.getMessage(), e);
        }

        if (info == null) {
            throw new RuntimeException("No payment info returned for transactionId: " + transactionId);
        }

        CreatePurchaseRequest req = new CreatePurchaseRequest();
        req.setTransactionId(transactionId);
        req.setUserId(info.getUserId());
        req.setSellerId(info.getSellerId()); // (Có thể null)
        req.setProductName(info.getProductName());
        req.setPrice(info.getPrice() == 0.0 ? info.getTotalAmount() : info.getPrice());
        req.setFullName(info.getFullName());
        req.setPhone(info.getPhone());
        req.setEmail(info.getEmail());
        req.setAddress(info.getAddress());
        req.setProductId(info.getProductId()); // (Rất có thể null)

        return createPurchase(req);
    }

    /**
     * HÀM CHUNG: Tạo Purchase (đã bao gồm bản vá DTO)
     */
    @Override
    @Transactional
    public Purchase createPurchase(CreatePurchaseRequest req) {
        if (req == null) throw new IllegalArgumentException("Request empty");

        Purchase.PurchaseBuilder builder = Purchase.builder()
                .transactionId(req.getTransactionId())
                .userId(req.getUserId())
                .sellerId(req.getSellerId()) // Áp dụng sellerId
                .productId(req.getProductId()) // Áp dụng productId
                .productName(req.getProductName())
                .price(req.getPrice())
                .fullName(req.getFullName()) // Áp dụng thông tin KH
                .phone(req.getPhone())
                .email(req.getEmail())
                .address(req.getAddress())
                .status(req.getStatus() == null ? "waiting_delivery" : req.getStatus())
                .createdAt(LocalDateTime.now());

        // Cố gắng làm giàu/vá lỗi sellerId nếu nó bị null (từ luồng REST)
        if (req.getProductId() != null) {
            try {
                log.info("Enriching purchase data from listing-service for productId: {}", req.getProductId());
                ProductDetailDTO pd = productClient.getProductDetail(req.getProductId());
                
                if (pd != null) {
                    if (pd.getProductName() != null) builder.productName(pd.getProductName());
                    if (pd.getPrice() != null) builder.price(pd.getPrice().doubleValue());

                    if (pd.getSeller() != null && pd.getSeller().getId() != null) {
                        builder.sellerId(pd.getSeller().getId().longValue()); 
                        log.info("Successfully enriched/overwritten sellerId via listing-service: {}", pd.getSeller().getId());
                    }

                    // Lấy ảnh
                    String imageToSave = null;
                    if (pd.getImageUrls() != null && !pd.getImageUrls().isEmpty()) {
                        imageToSave = pd.getImageUrls().get(0);
                    } else if (pd.getImages() != null && !pd.getImages().isEmpty()) {
                        imageToSave = pd.getImages().get(0).getUrl();
                    }
                    if (imageToSave != null) builder.productImage(imageToSave);
                }
            } catch (Exception e) {
                log.warn("Feign/Product service error (non-fatal) during enrichment: {}", e.getMessage());
            }
        }

        Purchase p = builder.build();
        
        if (p.getSellerId() == null) {
            log.warn("FINAL WARNING: sellerId is STILL NULL for transactionId: {}. Check 'order.paid' payload and listing-service response.", p.getTransactionId());
        }
        if (p.getFullName() == null) {
            log.warn("FINAL WARNING: customer_full_name is NULL for transactionId: {}. Check transaction-service REST response.", p.getTransactionId());
        }
        
        return purchaseRepository.save(p);
    }

    // (Các hàm còn lại giữ nguyên)

    @Override
    public List<Purchase> getPurchasesForUser(Long userId) {
        return purchaseRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Purchase> getPurchasesForUserByStatus(Long userId, String status) {
        return purchaseRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
    }

    @Override
    public Purchase getPurchaseById(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + id));
    }

    @Override
    public boolean existsByTransactionId(String transactionId) {
        if (transactionId == null) return false;
        return purchaseRepository.existsByTransactionId(transactionId);
    }

    @Override
    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }
}