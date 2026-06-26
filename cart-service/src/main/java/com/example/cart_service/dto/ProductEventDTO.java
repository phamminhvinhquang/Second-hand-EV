package com.example.cart_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

//DTO này dùng để nhận sự kiện khi một tin đăng bị xóa hoàn toàn.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEventDTO {
    private Long productId;
    private String eventType; // Ví dụ: "LISTING_DELETED"
}