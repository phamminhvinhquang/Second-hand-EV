package com.example.like_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "fcm_tokens", indexes = {
    @Index(name = "idx_userid", columnList = "userId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FcmToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String token;
    private String platform; // e.g. web, android, ios
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
