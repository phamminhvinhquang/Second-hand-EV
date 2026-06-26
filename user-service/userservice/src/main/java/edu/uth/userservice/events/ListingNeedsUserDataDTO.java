// File: src/main/java/edu/uth/userservice/dto/events/ListingNeedsUserDataDTO.java
package edu.uth.userservice.events;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingNeedsUserDataDTO {
    // DTO này User-Service dùng để NHẬN VỀ
    private Long listingId;
    private Long userId;
}