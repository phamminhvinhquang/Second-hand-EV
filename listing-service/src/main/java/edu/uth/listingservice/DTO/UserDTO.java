package edu.uth.listingservice.DTO;

import lombok.Data;

@Data
public class UserDTO {
    private Integer id; 
    private String name;
    private String email;
    private String phone;
    private String address;

}