package edu.uth.listingservice.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PricingRequestDTO {

  
    private String productType;
    private String brand;
    private Integer yearOfManufacture;
    private Integer conditionId;
    private String warrantyPolicy;
    private Integer mileage;
    private Integer rangePerCharge;
    private String batteryCapacity;
    private String batteryType;
    private Integer chargeCycles;
    private String batteryLifespan;


    private Integer maxSpeed;
    private String color;
    private String chargeTime;
    private String compatibleVehicle;

}