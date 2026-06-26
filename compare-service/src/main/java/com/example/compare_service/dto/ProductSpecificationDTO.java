package com.example.compare_service.dto;

import lombok.Data;

@Data
public class ProductSpecificationDTO {
    private Long specId;
    private Integer yearOfManufacture;
    private String brand;
    private ProductConditionDTO condition;
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
}