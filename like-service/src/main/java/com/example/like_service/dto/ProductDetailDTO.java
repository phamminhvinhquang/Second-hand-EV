package com.example.like_service.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {
    private Long productId;
    private String productName;
    private Long price;
    private List<String> imageUrls;
    private Integer yearOfManufacture;
    private String brand;

    // trực tiếp conditionName nếu trả phẳng
    private String conditionName;
    private Long mileage;

    // trực tiếp sellerId nếu trả phẳng
    @JsonProperty("sellerId")
    private Long sellerId;

    // nested possibilities
    private SellerDTO seller;
    private ListingDTO listing;

    public Long resolveSellerId() {
        if (sellerId != null) return sellerId;
        if (seller != null && seller.getId() != null) return seller.getId();
        if (listing != null && listing.getUserId() != null) return listing.getUserId();
        return null;
    }

    // nested condition object if present
    private ConditionDTO condition;

    public String getEffectiveConditionName() {
        String nested = (condition == null) ? null : condition.getConditionName();
        if (hasText(nested)) return nested;
        return hasText(conditionName) ? conditionName : null;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    @Getter @Setter @NoArgsConstructor
    public static class SellerDTO { private Long id; }

    @Getter @Setter @NoArgsConstructor
    public static class ListingDTO { @JsonProperty("userId") private Long userId; }

    @Getter @Setter @NoArgsConstructor
    public static class ConditionDTO { private String conditionName; }
}
