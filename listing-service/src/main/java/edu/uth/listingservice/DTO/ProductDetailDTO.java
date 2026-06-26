
package edu.uth.listingservice.DTO;

import edu.uth.listingservice.Model.Product;
import edu.uth.listingservice.Model.ProductImage;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Model.ProductSpecification;
import edu.uth.listingservice.Model.ListingStatus; 
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class ProductDetailDTO {

    // --- Thông tin từ Product ---
    private Long productId;
    private String productName;
    private String productType;
    private Long price;
    private String description;
   

    // --- Thông tin từ ProductImage ---
    private List<String> imageUrls;

    // --- Thông tin từ ProductSpecification ---
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

    // --- Thông tin từ ProductCondition (lấy qua Specification) ---
    private String conditionName;

    // --- Thông tin từ ProductListing ---
    private String phone;
    private String location;
    private boolean verified; 
    private ListingStatus listingStatus; 

    // --- Thông tin người bán ---
    private UserDTO seller;

    // Constructor to easily convert from Entities
    public ProductDetailDTO(Product product, List<ProductImage> images, ProductSpecification spec, ProductListing listing, UserDTO seller) {
        // Mapping from Product
        this.productId = product.getProductId();
        this.productName = product.getProductName();
        this.productType = product.getProductType();
        this.price = product.getPrice();
        this.description = product.getDescription();
    

        // Mapping from List<ProductImage>
        if (images != null) {
            this.imageUrls = images.stream()
                                   .map(ProductImage::getImageUrl)
                                   .collect(Collectors.toList());
        }

        // Mapping from ProductSpecification
        if (spec != null) {
            this.yearOfManufacture = spec.getYearOfManufacture();
            this.brand = spec.getBrand();
            this.mileage = spec.getMileage();
            this.batteryCapacity = spec.getBatteryCapacity();
            this.batteryType = spec.getBatteryType();
            this.batteryLifespan = spec.getBatteryLifespan();
            this.compatibleVehicle = spec.getCompatibleVehicle();
            this.warrantyPolicy = spec.getWarrantyPolicy();
            this.maxSpeed = spec.getMaxSpeed();
            this.rangePerCharge = spec.getRangePerCharge();
            this.color = spec.getColor();
            this.chargeTime = spec.getChargeTime();
            this.chargeCycles = spec.getChargeCycles();

            // Get ConditionName from within Specification
            if (spec.getCondition() != null) {
                this.conditionName = spec.getCondition().getConditionName();
            }
        }

        // Mapping from ProductListing
        if (listing != null) {
            this.phone = listing.getPhone();
            this.location = listing.getLocation();
            this.verified = listing.isVerified(); 
            this.listingStatus = listing.getListingStatus(); 
        }

        // Mapping from UserDTO
        this.seller = seller;
    }
}

