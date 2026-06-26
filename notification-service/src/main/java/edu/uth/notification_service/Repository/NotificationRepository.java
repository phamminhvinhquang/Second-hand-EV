// Tạo file mới: edu/uth/notification_service/Repository/NotificationRepository.java
package edu.uth.notification_service.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import edu.uth.notification_service.Model.Notification;
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy thông báo theo phân trang
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Đếm số thông báo CHƯA ĐỌC
    long countByUserIdAndIsReadFalse(Long userId);

    // Tìm tất cả thông báo CHƯA ĐỌC
    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    // Đánh dấu tất cả là ĐÃ ĐỌC
    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(Long userId);

    /**
     *  THÊM PHƯƠNG THỨC NÀY
     * Xóa tất cả thông báo của một người dùng
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}