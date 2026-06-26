package edu.uth.listingservice.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Product_Specifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "spec_id")
    private Long specId;

    @OneToOne
    @JoinColumn(name = "product_id")
    @JsonBackReference
    private Product product;

    private Integer yearOfManufacture;
    private String brand;

    @ManyToOne
    @JoinColumn(name = "condition_id")
    private ProductCondition condition;

    private Long mileage;
    private String batteryCapacity;
    private String batteryType;
    private String batteryLifespan;
    private String compatibleVehicle;
    private String warrantyPolicy;
    private Integer maxSpeed;

    @Column(name = "range_per_charge")
    private Integer rangePerCharge;

    private String color;
    
    //  NEW FIELDS ADDED BELOW
    
    @Column(name = "charge_time")
    private String chargeTime; // Ví dụ: "6-8 giờ", "45 phút"

    @Column(name = "charge_cycles")
    private Long chargeCycles; // Ví dụ: 500, 1000
}