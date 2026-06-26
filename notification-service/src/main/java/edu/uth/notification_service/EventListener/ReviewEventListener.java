
package edu.uth.notification_service.EventListener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.uth.notification_service.DTO.ReviewCreatedDTO;
import edu.uth.notification_service.Service.NotificationService;

@Component
public class ReviewEventListener {

    @Autowired
    private NotificationService notificationService;

   
    @Value("${app.frontend.base-url:http://localhost:9000}")
    private String frontendBaseUrl;

    @RabbitListener(queues = "review.created.queue")
    public void handleReviewCreated(ReviewCreatedDTO event) {
        try {
            if (event == null || event.getReviewedPartyId() == null) {
                return;
            }

            System.out.println("📨 [Notification] Nhận đánh giá mới cho User ID: " + event.getReviewedPartyId());

            // 1. Xử lý tên người đánh giá
            String reviewerName = event.getReviewerName();
            if (reviewerName == null || reviewerName.trim().isEmpty()) {
                reviewerName = "Một khách hàng"; 
            }
            
            // 2. Xử lý rating (Lấy trực tiếp từ DTO mới sửa)
            int rating = event.getRating();
            if (rating <= 0) rating = 5; // Fallback an toàn nếu lỗi
            
            // Nội dung: "Nguyễn Văn A đã đánh giá 5 sao cho bạn."
            String message = String.format("%s đã đánh giá %d sao cho bạn.", reviewerName, rating);

            // 3. SỬA ĐƯỜNG DẪN (QUAN TRỌNG)
            // - Trỏ về edit_news.html (File hồ sơ của bạn)
            // - Thêm id của người được đánh giá (để trang web biết load profile nào)
            // - Thêm tab=REVIEWS để tự động chuyển tab
            String link = String.format("%s/edit_news.html?id=%d&tab=REVIEWS", 
                                        frontendBaseUrl, 
                                        event.getReviewedPartyId());

            notificationService.createNotification(event.getReviewedPartyId(), message, link);

        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý Review Notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}