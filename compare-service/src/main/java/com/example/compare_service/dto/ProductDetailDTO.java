package com.example.compare_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductDetailDTO {
    private Long productId;
    private String productName;
    private String productType;
    private Long price;
    private String description;
    private String status;
    private List<String> imageUrls;
    private Integer yearOfManufacture;
    private String brand;
    private Long mileage;
    private String batteryCapacity;
    private String batteryType;
    private String batteryLifespan;
    private String compatibleVehicle;
    private String warrantyPolicy;
    private Integer maxSpeed;
    private Integer rangePerCharge;
    private String color;
    private String chargeTime;
    private Long chargeCycles;
    private String conditionName;
    private String phone;
    private String location;
    // seller thông tin có thể giữ Object hoặc map
    private Object seller;
}
