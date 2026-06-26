package edu.uth.listingservice.DTO; 

import lombok.Data;

@Data
public class UpdateListingDTO {
    // Thông tin cơ bản
    private String productName;
    private String brand;
    private Long price;
    private String description;
    private String phone;
    private String location;
    private String warrantyPolicy;

    // --- CÁC TRƯỜNG KỸ THUẬT ĐƯỢC ĐỒNG BỘ VỚI MODEL ---
    
    // Các trường kiểu String
    private String batteryCapacity;
    private String batteryType;
    private String batteryLifespan;
    private String compatibleVehicle;
    private String color;
    private String chargeTime;
    
    // Các trường kiểu Integer
    private Integer yearOfManufacture;
    private Integer maxSpeed;
    private Integer rangePerCharge;

    // Các trường kiểu Long
    private Long mileage;
    private Long chargeCycles;
}