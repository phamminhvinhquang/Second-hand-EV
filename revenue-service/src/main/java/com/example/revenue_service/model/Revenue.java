package com.example.revenue_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "revenues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Revenue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_id", unique = true)
    private Long purchaseId; // ID giao dịch gốc từ Purchase Service

    @Column(name = "seller_id")
    private Long sellerId; // ID người bán

    @Column(name = "amount")
    private Double amount; // Tổng số tiền giao dịch

    @Column(name = "revenue_date")
    private LocalDate revenueDate; // Ngày phát sinh doanh thu (để tính toán)

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}