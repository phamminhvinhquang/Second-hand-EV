package edu.uth.example.review_service.DTO;
import lombok.Data;

@Data
public class ReviewRequestDTO {
   
    private Long productId; 
    
    private int rating;     
    private String comment;
    private String reviewerName; 
}