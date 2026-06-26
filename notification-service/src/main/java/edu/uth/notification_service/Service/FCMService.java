
package edu.uth.notification_service.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;

import edu.uth.notification_service.Model.Notification;

@Service
public class FCMService {

    @Autowired
    private FirebaseMessaging firebaseMessaging;

    @Autowired
    private UserDeviceService userDeviceService;


    public void sendPushNotification(Notification notification) {
        List<String> deviceTokens = userDeviceService.getTokensByUserId(notification.getUserId());

        if (deviceTokens == null || deviceTokens.isEmpty()) {
            System.err.println("Không tìm thấy token cho User ID: " + notification.getUserId());
            return;
        }
        
        if (deviceTokens.size() > 500) {
             deviceTokens = deviceTokens.subList(0, 500);
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(deviceTokens) 
                .putData("title", "Bạn có thông báo mới!")
                .putData("body", notification.getMessage())
                .putData("link", notification.getLink())
                .putData("notificationId", notification.getId().toString())
                .putData("type", "system") // Đánh dấu là thông báo hệ thống
                .putData("image", "http://localhost:9000/images/logo.png") 
                .build();

        try {
            firebaseMessaging.sendEachForMulticast(message);
        } catch (FirebaseMessagingException e) {
            System.err.println("Lỗi gửi FCM: " + e.getMessage());
        }
    }

    // --- [MỚI] HÀM GỬI THÔNG BÁO CHAT (Data-only) ---
    public void sendPushNotificationToUser(Long userId, String title, String body, String link) {
        // 1. Lấy token (Tái sử dụng hàm getTokensByUserId của bạn)
        List<String> deviceTokens = userDeviceService.getTokensByUserId(userId);

        if (deviceTokens == null || deviceTokens.isEmpty()) {
            // User đang offline hoặc chưa đăng ký thiết bị -> Không gửi
            return;
        }

        if (deviceTokens.size() > 500) {
            deviceTokens = deviceTokens.subList(0, 500);
        }

        // 2. Tạo Message Data-only
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(deviceTokens)
                .putData("title", title)
                .putData("body", body)
                .putData("link", link)
                .putData("type", "chat") 
                .putData("image", "http://localhost:9000/images/logo.png") 
                .build();

        // 3. Gửi
        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            System.out.println("📨 Gửi FCM Chat thành công: " + response.getSuccessCount() + " thiết bị.");
        } catch (FirebaseMessagingException e) {
            System.err.println("❌ Lỗi gửi FCM Chat: " + e.getMessage());
        }
    }
}