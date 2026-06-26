// File: edu/uth/example/review_service/EventListener/OrderEventListener.java
package edu.uth.example.review_service.EventListener;

import org.slf4j.Logger; // ⭐️ Thêm Logger
import org.slf4j.LoggerFactory; // ⭐️ Thêm Logger
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.uth.example.review_service.DTO.OrderCompletedEventDTO;
import edu.uth.example.review_service.Service.ReviewService;

@Component // ⭐️ Bỏ comment
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @Autowired
    private ReviewService reviewService;

    /**
     * Lắng nghe sự kiện từ TransactionService khi một sản phẩm hoàn tất thanh toán.
     */
    // ⭐️ Trỏ vào queueName đã khai báo trong application.properties
    @RabbitListener(queues = "${app.rabbitmq.order-events.queue}")
    public void handleOrderCompleted(OrderCompletedEventDTO dto) {
        
        if (dto == null || dto.getProductId() == null) {
            log.warn("❌ [MQ] Nhận được sự kiện order.paid không hợp lệ (thiếu productId).");
            return;
        }
        
        log.info("✅ [MQ] Nhận sự kiện OrderCompleted cho productId: {}", dto.getProductId());
        try {
            // Gọi service để tạo giao dịch có thể đánh giá
            reviewService.createReviewableTransaction(dto);
            log.info("✅ Đã tạo ReviewableTransaction cho productId: {}", dto.getProductId());
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý sự kiện order completed cho productId: {}: {}", dto.getProductId(), e.getMessage());
            // (Cần xử lý retry hoặc dead-letter-queue sau)
        }
    }
}