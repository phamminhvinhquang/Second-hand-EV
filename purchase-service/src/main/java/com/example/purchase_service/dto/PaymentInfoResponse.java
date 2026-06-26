package com.example.purchase_service.dto;

import lombok.Data;

@Data
public class PaymentInfoResponse {
    private String transactionId;
    private String status; // SUCCESS / FAILED / PENDING / CANCELED
    private String method;
    // customer
    private String fullName;
    private String phone;
    private String email;
    private String address;
    // product/cart info
    private String productName;
    private double price;
    private double totalAmount;
    private String type; // "order" or "deposit"
    private Long userId;
    private Long sellerId;
    private Long productId;
}
