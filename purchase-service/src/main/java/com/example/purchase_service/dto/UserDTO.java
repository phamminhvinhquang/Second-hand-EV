package com.example.purchase_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO này dùng để hứng đối tượng "seller" trả về từ listing-service.
 * Nó phải khớp với cấu trúc JSON mà listing-service gửi đi.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // Bỏ qua các trường không cần thiết (như roles, password...)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    // Tên trường "id" phải khớp với JSON trả về từ listing-service
    private Integer id; 
    
    private String name;
    private String email;
    private String phone;
    private String address;
}