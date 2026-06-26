package com.example.purchase_service.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseDTO {
    private Long id;
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
    private LocalDateTime createdAt;
    private List<String> imageUrls;
}
