package com.example.purchase_service.model;

import lombok.Builder;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_id")
    private Long complaintId;

    @Column(length = 20)
    private String sender; // "USER" or "ADMIN"

    @Column(name = "sender_name", length = 200)
    private String senderName;

    @Column(length = 2000)
    private String content;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
