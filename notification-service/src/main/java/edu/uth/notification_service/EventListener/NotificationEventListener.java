
package edu.uth.notification_service.EventListener;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.uth.notification_service.DTO.ListingEventDTO;
import edu.uth.notification_service.Service.NotificationService;

@Component
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService; 

    private static final String NOTIFICATION_QUEUE = "${app.rabbitmq.queue}";

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl; 

    @RabbitListener(queues = NOTIFICATION_QUEUE)
    public void handleListingEvent(ListingEventDTO event) { 
        
        System.out.println("Đã hứng được sự kiện: " + event.getEventType() + " cho User: " + event.getUserId());

        String message;
        String link;
        
        // Cấu trúc link mới: /file?param=value&tab=STATUS
        String targetFile = "/edit_news.html"; 
        String parameterName = "?listing_id=";
        String tabParameter = "&tab="; // <-- THAM SỐ MỚI

        // Xử lý logic dựa trên loại sự kiện
        switch (event.getEventType()) { 
            case "APPROVED":
                message = String.format("Tin đăng '%s' của bạn đã được duyệt.", event.getProductName());
                // Link đến tab "Đang hiển thị" (ACTIVE)
                link = String.format("%s%s%s%d%sACTIVE", frontendBaseUrl, targetFile, parameterName, event.getListingId(), tabParameter);
                break;
            case "REJECTED":
                message = String.format("Tin đăng '%s' đã bị từ chối.", event.getProductName());
                // Link đến tab "Bị từ chối" (REJECTED)
                link = String.format("%s%s%s%d%sREJECTED", frontendBaseUrl, targetFile, parameterName, event.getListingId(), tabParameter);
                break;
            case "SOLD":
                message = String.format("Sản phẩm '%s' của bạn đã được bán.", event.getProductName());
                // Link đến tab "Đã bán" (SOLD)
                link = String.format("%s%s%s%d%sSOLD", frontendBaseUrl, targetFile, parameterName, event.getListingId(), tabParameter);
                break;
            case "VERIFIED":
                 message = String.format("Tin đăng '%s' đã được kiểm định.", event.getProductName());
                 // Link đến tab "Đang hiển thị" (ACTIVE)
                 link = String.format("%s%s%s%d%sACTIVE", frontendBaseUrl, targetFile, parameterName, event.getListingId(), tabParameter);
                 break;
            default:
                System.out.println("Sự kiện không xác định (bỏ qua): " + event.getEventType());
                return; // Bỏ qua
        }

        // Gọi NotificationService để tạo thông báo
        notificationService.createNotification(event.getUserId(), message, link);
    }
}