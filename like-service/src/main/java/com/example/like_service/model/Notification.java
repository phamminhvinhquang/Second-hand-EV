package com.example.like_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user", columnList = "userId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;       // recipient
    private String title;
    @Column(length = 2000)
    private String body;
    private Long productId;
    private String link;       // ví dụ "/product_detail.html?id=123"
    private boolean seen = false;
    private Instant createdAt;

    @Column(name = "product_name", length = 255)
    private String productName;
    
    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
