// File: src/main/java/edu/uth/userservice/dto/events/UserDataResponseDTO.java
package edu.uth.userservice.events;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDataResponseDTO {
    // DTO này User-Service dùng để GỬI ĐI
    private Long listingId;
    private Integer userId;
    private String name;
    private String email;
    private String phone;
    private String address;
}