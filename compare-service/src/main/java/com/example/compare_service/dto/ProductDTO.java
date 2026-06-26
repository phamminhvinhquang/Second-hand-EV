package com.example.compare_service.dto;


import lombok.Data;
import java.util.List;

@Data
public class ProductDTO {
    private Long productId;
    private String productName;
    private String productType;
    private Long price;
    private Long aiSuggestedPrice;
    private String description;
    private String status;
    private List<ProductImageDTO> images;
    private ProductSpecificationDTO specification;
}