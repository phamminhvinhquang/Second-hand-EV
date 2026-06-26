
package edu.uth.listingservice.events;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingNeedsUserDataDTO {
   
    private Long listingId;
    private Long userId;
}