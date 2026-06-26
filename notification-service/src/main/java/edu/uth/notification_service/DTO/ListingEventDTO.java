
package edu.uth.notification_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingEventDTO {
    private Long listingId;
    private Long userId;
    private String productName;
    private String eventType; 
}