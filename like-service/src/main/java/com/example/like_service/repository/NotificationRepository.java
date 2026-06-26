package com.example.like_service.repository;

import com.example.like_service.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Notification> findTopByUserIdAndProductIdOrderByCreatedAtDesc(Long userId, Long productId);
}
