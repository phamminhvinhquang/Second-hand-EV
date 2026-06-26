package com.example.search_service.dto;
import lombok.Data;
import java.util.Date;

@Data
public class ProductListingDTO {
    private Long listingId;
    private ProductDTO product;
    private Long userId;
    private String phone;
    private String location;
    private String listingStatus;
    private Date listingDate;
    private Date updatedAt;
    private boolean updatedOnce;
}
