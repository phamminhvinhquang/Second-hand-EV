// File: edu/uth/notificationservice/Repository/UserDeviceTokenRepository.java
package edu.uth.notification_service.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uth.notification_service.Model.UserDeviceToken;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

    // (Giữ nguyên) Dùng để gửi thông báo (1 user, nhiều token)
    List<UserDeviceToken> findByUserId(Long userId);

    // ✅ NÂNG CẤP: Dùng để kiểm tra khi đăng nhập VÀ để xóa khi đăng xuất
    Optional<UserDeviceToken> findByUserIdAndDeviceToken(Long userId, String deviceToken);
    
    // (Hàm 'findByDeviceToken(String token)' đã được thay thế bằng hàm trên)
}