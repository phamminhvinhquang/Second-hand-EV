package com.example.purchase_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaint_purchase_id", columnList = "purchase_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_id")
    private Long purchaseId;

    @Column(name = "sender_name", length = 200)
    private String senderName;

    @Column(name = "sender_phone", length = 60)
    private String senderPhone;

    @Column(name = "sender_email", length = 200)
    private String senderEmail;

    @Column(name = "detail", length = 2000)
    private String detail;

    // NEW, READ, RESOLVED
    @Column(length = 30)
    private String status = "NEW";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /* --- admin reply fields --- */
    @Column(name = "admin_response", length = 2000)
    private String adminResponse;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;
}
