
package edu.uth.notification_service.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReviewCreatedDTO {
    private Long reviewedPartyId; 
    private Long reviewerId;      
    private String reviewerName;  
    private int rating;           
    private String comment;       
}