package com.example.revenue_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO dùng để nhận dữ liệu Purchase từ purchase-service (hoặc NiFi).
 * Các trường nên tương thích với JSON mà purchase-service trả về.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PurchaseDTO {
    private Long id; // purchase id
    private String transactionId;
    private Long userId;
    private Long sellerId;
    private Long productId;
    private String productName;
    private String productImage;
    private Double price;
    private String status;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private String createdAt; // ISO string (e.g. 2025-11-19T08:12:34)
}