package edu.uth.example.review_service.DTO;

import com.fasterxml.jackson.annotation.JsonAlias; 
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OrderCompletedEventDTO {
    private Long sellerId;

   
    @JsonAlias("userId") 
    private Long buyerId;
    
    private String productName;
    private Long price;
    
    private Long productId;
}