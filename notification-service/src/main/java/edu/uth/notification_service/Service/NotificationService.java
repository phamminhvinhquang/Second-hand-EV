package edu.uth.notification_service.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import edu.uth.notification_service.Model.Notification;
import edu.uth.notification_service.Repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationService {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private FCMService fcmService;
    @Autowired private CacheManager cacheManager;

    // 1. Cache danh sách: Key kết hợp userId, page, size
    @Cacheable(value = "user_notifications", key = "#userId + '-' + #page + '-' + #size")
    public Page<Notification> getNotificationsForUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    // 2. Cache số lượng chưa đọc
    @Cacheable(value = "unread_count", key = "#userId")
    public long getUnreadNotificationCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public Notification createNotification(Long userId, String message, String link) {
        Notification notification = new Notification(userId, message, link);
        Notification savedNotification = notificationRepository.save(notification);

        // Xóa Cache sau khi commit DB thành công
        evictUserCaches(userId);

        // Gửi Socket
        messagingTemplate.convertAndSendToUser(
            String.valueOf(userId),
            "/topic/notifications",
            savedNotification
        );

        // Gửi Push
        try {
            fcmService.sendPushNotification(savedNotification);
        } catch (Exception e) {
            log.error("Lỗi FCM: {}", e.getMessage());
        }
        
        return savedNotification;
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        evictUserCaches(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
            evictUserCaches(notification.getUserId());
        });
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
             Long userId = notification.getUserId();
             notificationRepository.deleteById(notificationId);
             evictUserCaches(userId);
        });
    }
    
    @Transactional
    public void deleteAllForUser(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
        evictUserCaches(userId);
    }

    // Hàm helper xóa cache
    private void evictUserCaches(Long userId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Xóa số lượng chưa đọc
                cacheManager.getCache("unread_count").evictIfPresent(userId);
                
                // Xóa toàn bộ cache danh sách thông báo (vì không biết chính xác page nào thay đổi)
                cacheManager.getCache("user_notifications").clear();
                
                log.info("✅ Đã xóa cache Notification cho User ID: {}", userId);
            }
        });
    }
}