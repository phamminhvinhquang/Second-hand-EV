package com.example.like_service.controller;

import com.example.like_service.model.Notification;
import com.example.like_service.repository.NotificationRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notif")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return ResponseEntity.status(401).build();
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/mark-read")
    public ResponseEntity<?> markRead(@PathVariable Long id, @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return ResponseEntity.status(401).build();
        return notificationRepository.findById(id).map(n -> {
            if (!n.getUserId().equals(userId)) return ResponseEntity.status(403).build();
            n.setSeen(true);
            notificationRepository.save(n);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id,
                                                @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return ResponseEntity.status(401).build();

        Optional<Notification> opt = notificationRepository.findById(id);
        if (!opt.isPresent()) {
            // Nếu không tìm thấy -> trả 404 (frontend cũng sẽ coi là "đã bị xóa")
            return ResponseEntity.notFound().build();
        }

        Notification n = opt.get();
        if (!n.getUserId().equals(userId)) {
            // User cố gắng xóa notification của người khác
            return ResponseEntity.status(403).build();
        }

        notificationRepository.deleteById(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
