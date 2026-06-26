// Tạo file mới: edu/uth/notification_service/Controller/NotificationController.java
package edu.uth.notification_service.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uth.notification_service.Model.Notification;
import edu.uth.notification_service.Service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // API cho chuông thông báo (lấy 5-10 tin mới nhất)
    @GetMapping("/user/{userId}")
    public Page<Notification> getNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return notificationService.getNotificationsForUser(userId, page, size);
    }

    // API cho dấu chấm đỏ (chỉ lấy số lượng)
    @GetMapping("/user/{userId}/unread-count")
    public long getUnreadCount(@PathVariable Long userId) {
        return notificationService.getUnreadNotificationCount(userId);
    }

  //  Sửa lại hàm này
    @PostMapping("/mark-all-as-read/user/{userId}")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        // Trả về 204 No Content thay vì 200 OK với body rỗng
        return ResponseEntity.noContent().build();
    }

    //  Sửa lại hàm này
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markOneAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        // Trả về 204 No Content
        return ResponseEntity.noContent().build();
    }

    //  Sửa lại hàm này (DELETE thường trả về 204)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        // Trả về 204 No Content
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllNotifications(@PathVariable Long userId) {
        notificationService.deleteAllForUser(userId);
        return ResponseEntity.noContent().build();
    }
}