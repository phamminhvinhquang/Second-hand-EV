
package edu.uth.listingservice.events;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDataResponseDTO {
    // DTO này Listing-Service dùng để NHẬN VỀ
    private Long listingId;
    private Integer userId;
    private String name;
    private String email;
    private String phone;
    private String address;
}