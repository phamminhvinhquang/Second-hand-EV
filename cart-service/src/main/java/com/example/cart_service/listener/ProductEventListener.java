package com.example.cart_service.listener;

import com.example.cart_service.config.RabbitMQConfig;
import com.example.cart_service.repository.CartRepository;
import com.example.cart_service.client.ProductServiceClient;

import feign.FeignException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component
public class ProductEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductServiceClient productClient;

    // Listener nhận các event liên quan product/listing từ RabbitMQ.
    // Payload được accept dưới dạng Map để tránh lỗi khi cấu trúc event khác nhau.
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME, containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void onProductMessage(Map<String, Object> payload) {
        log.info("Received raw MQ payload: {}", payload);
        if (payload == null || payload.isEmpty()) return;

        // cố gắng lấy productId từ nhiều dạng (Number, String)
        Long productId = extractProductId(payload);
        String eventType = payload.get("eventType") != null ? String.valueOf(payload.get("eventType")) : null;
        String listingStatus = payload.get("listingStatus") != null ? String.valueOf(payload.get("listingStatus")) : null;

        log.info("Parsed eventType={}, listingStatus={}, productId={}", eventType, listingStatus, productId);

        try {
            // 1) Nếu eventType rõ ràng là LISTING_DELETED =>xóa ngay các cart liên quan.
            if (eventType != null && "LISTING_DELETED".equalsIgnoreCase(eventType)) {
                int deleted = cartRepository.deleteByProductId(productId);
                log.info("Deleted {} cart rows for productId={} due to LISTING_DELETED", deleted, productId);
                return;
            }

            // 2) Nếu listingStatus báo SOLD/REJECTED => xóa cart ngay.
            if (listingStatus != null && (listingStatus.equalsIgnoreCase("SOLD") || listingStatus.equalsIgnoreCase("REJECTED"))) {
                int deleted = cartRepository.deleteByProductId(productId);
                log.info("Deleted {} cart rows for productId={} due to listingStatus={}", deleted, productId, listingStatus);
                return;
            }

            // Fallback: nếu có productId, gọi product-service để verify.
            // Nếu product-service trả 404 -> xóa cart; nếu lỗi khác -> log để điều tra.
            if (productId != null) {
                try {
                    productClient.getProductDetail(productId); // nếu OK -> tồn tại -> không làm gì
                    log.debug("Product {} exists according to product-service", productId);
                } catch (FeignException fe) {
                    if (fe.status() == 404) {
                        int deleted = cartRepository.deleteByProductId(productId);
                        log.info("Deleted {} cart rows for productId={} because product-service returned 404", deleted, productId);
                    } else {
                        log.error("Feign error while verifying productId={}: status={}, msg={}", productId, fe.status(), fe.getMessage());
                    }
                } catch (Exception ex) {
                    log.error("Unexpected error while verifying productId={}: {}", productId, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error handling product event payload {}: {}", payload, e.getMessage(), e);
        }
    }
    
    // Thử extract productId từ payload (hỗ trợ Number hoặc String; thử key "productId" rồi "id").
    private Long extractProductId(Map<String, Object> payload) {
        Object idObj = payload.get("productId");
        if (idObj == null) idObj = payload.get("id"); // thử key khác
        if (idObj == null) return null;
        try {
            if (idObj instanceof Number) {
                return ((Number) idObj).longValue();
            }
            return Long.valueOf(String.valueOf(idObj));
        } catch (Exception e) {
            log.warn("Could not parse productId from payload value={} : {}", idObj, e.toString());
            return null;
        }
    }
}
