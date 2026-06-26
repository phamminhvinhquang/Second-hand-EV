package com.example.purchase_service.dto;

import lombok.Data;

@Data
public class CreateComplaintRequest {
    private Long purchaseId;
    private String senderName;
    private String senderPhone;
    private String senderEmail;
    private String detail;
}
