
package edu.uth.example.review_service.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.uth.example.review_service.DTO.UserEventDTO;
import edu.uth.example.review_service.Service.ReviewService;

@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    @Autowired
    private ReviewService reviewService;

    @RabbitListener(queues = "${app.rabbitmq.user.queue.listener}")
    public void handleUserEvent(UserEventDTO userEvent) {
        if (userEvent == null || userEvent.getId() == null) {
            log.warn("Nhận được sự kiện user không hợp lệ (null hoặc thiếu ID).");
            return;
        }

        
        log.info("Nhận được sự kiện đồng bộ User (từ user.#) cho User ID: {}", userEvent.getId());
        
        // Thêm kiểm tra 'name' để đảm bảo an toàn
        if (userEvent.getName() == null || userEvent.getName().isEmpty()) {
            log.warn("Tên user trong sự kiện bị rỗng, bỏ qua cập nhật. User ID: {}", userEvent.getId());
            return;
        }
        
        try {
            reviewService.updateReviewerName(
                userEvent.getId().longValue(), 
                userEvent.getName()
            );
            log.info("Đã đồng bộ tên người đánh giá cho User ID: {}", userEvent.getId());
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ tên người đánh giá cho User ID: {}: {}", userEvent.getId(), e.getMessage());
        }
       
    }
}