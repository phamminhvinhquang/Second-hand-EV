package edu.uth.notification_service.EventListener;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload; 
import org.springframework.stereotype.Component;

import edu.uth.notification_service.DTO.NewMessageEventDTO;
import edu.uth.notification_service.Service.FCMService;

@Component
public class ChatEventListener {

    @Autowired
    private FCMService fcmService;

    @Value("${app.frontend.base-url:http://localhost:9000}") 
    private String frontendBaseUrl;

    @RabbitListener(queues = "notification.chat.queue")
    public void handleChatMessage(@Payload NewMessageEventDTO event) {
        try {
            System.out.println("📨 [Notification] Xử lý tin nhắn từ: " + event.getSenderName());

            String title = "Tin nhắn mới từ " + event.getSenderName();
            String rawContent = event.getContent();
            String displayBody;

            // --- LOGIC XỬ LÝ NỘI DUNG THÔNG MINH HƠN ---
            
            if (rawContent == null) {
                displayBody = "Đã gửi một tin nhắn.";
            } 
            // 1. Kiểm tra nếu là Ảnh (dựa vào đuôi file hoặc đường dẫn uploads)
            else if (isImage(rawContent)) {
                displayBody = "[Đã gửi một hình ảnh] ";
            }
            // 2. Kiểm tra nếu là Video
            else if (isVideo(rawContent)) {
                displayBody = "[Đã gửi một video] ";
            }
            // 3. Kiểm tra nếu là JSON Sản phẩm (bắt đầu bằng "{" và có chứa "price" hoặc "id")
            else if (rawContent.trim().startsWith("{") && rawContent.contains(":")) {
                try {
                    // Thử parse JSON sơ bộ hoặc đơn giản là gán text cứng
                    displayBody = "[Đã chia sẻ một sản phẩm] ";
                } catch (Exception e) {
                    displayBody = rawContent; // Fallback nếu không phải JSON
                }
            }
            // 4. Tin nhắn văn bản thường
            else {
                // Cắt ngắn nếu quá dài
                if (rawContent.length() > 50) {
                    displayBody = rawContent.substring(0, 50) + "...";
                } else {
                    displayBody = rawContent;
                }
            }
            // ---------------------------------------------

            // TẠO LINK CHAT
            String encodedName = URLEncoder.encode(event.getSenderName(), StandardCharsets.UTF_8);
            String link = String.format("%s/chat.html?to=%d&name=%s", 
                                         frontendBaseUrl, 
                                         event.getSenderId(), 
                                         encodedName);

            // Gửi sang FCM Service
            fcmService.sendPushNotificationToUser(event.getRecipientId(), title, displayBody, link);

        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý sự kiện Chat FCM: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private boolean isImage(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        return (lower.contains("/uploads/") || lower.contains("/chat-files/")) && 
               (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp"));
    }

    
    private boolean isVideo(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        return (lower.contains("/uploads/") || lower.contains("/chat-files/")) && 
               (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi"));
    }
}