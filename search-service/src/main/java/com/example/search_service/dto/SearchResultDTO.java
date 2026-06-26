package com.example.search_service.dto;
import java.util.List;

import lombok.Data;

@Data
public class SearchResultDTO {
    private Long productId;
    private String productName;
    private String productType;
    private Long price;
    private List<String> imageUrls;
    private String location;
    private Integer yearOfManufacture;
    private String brand;
    private String batteryCapacity;
    private Long mileage;
    private String conditionName;
    private String batteryType;
}
