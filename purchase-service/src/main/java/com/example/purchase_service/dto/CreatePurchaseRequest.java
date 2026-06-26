package com.example.purchase_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreatePurchaseRequest {
    private String transactionId; // optional
    private Long userId;
    private Long sellerId;
    private Long productId;
    private String productName;
    private Double price;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private String status;
}
