package com.example.compare_service.dto;

import lombok.Data;

@Data
public class ProductImageDTO {
    private Long imageId;
    private String imageUrl;
    private String imageType;
}