package com.example.cart_service.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


// DTO nhận chi tiết product từ listing-service (dùng khi gọi remote service).
// @JsonIgnoreProperties => nếu service trả thêm trường lạ thì không lỗi.
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {
    // các trường
    private Long productId;
    private String productName;
    private Long price; 
    private List<String> imageUrls;
    private Integer yearOfManufacture;
    private String brand;
    private String conditionName; 
    private Long mileage;

    private String listingStatus;

    // sellerId có thể trả trực tiếp hoặc nằm trong nested seller/listing
    @JsonProperty("sellerId")
    private Long sellerId;

    private SellerDTO seller;  // nested object nếu service trả seller chi tiết
    private ListingDTO listing; // nested object nếu service trả listing info

    /**
     * Trả sellerId đã được resolve theo thứ tự ưu tiên:
     * 1) trường sellerId trực tiếp
     * 2) nested seller.id
     * 3) nested listing.userId
     * Nếu không tìm được -> trả null.
     */
    public Long resolveSellerId() {
        if (sellerId != null) return sellerId;
        if (seller != null && seller.getId() != null) return seller.getId();
        if (listing != null && listing.getUserId() != null) return listing.getUserId();
        // thêm fallback khác nếu cần
        return null;
    }

    @Getter @Setter @NoArgsConstructor
    public static class SellerDTO { private Long id; }

    @Getter @Setter @NoArgsConstructor
    public static class ListingDTO { @JsonProperty("userId") private Long userId; }
    // nếu product-service trả nested: "condition": { "conditionName": "Mới 99% (Lướt)" }
    
    
    private ConditionDTO condition;
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConditionDTO {
        private String conditionName;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * Trả tên condition thực tế sử dụng thứ tự:
     * 1) nested condition.conditionName (nếu có)
     * 2) field conditionName (nếu có)
     * 3) null nếu không có thông tin condition
     */
    public String getEffectiveConditionName() {
        String nested = (condition == null) ? null : condition.getConditionName();
        if (hasText(nested)) return nested;
        return hasText(conditionName) ? conditionName : null;
    }
}
