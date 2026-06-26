
package edu.uth.notification_service.DTO;

import lombok.Data;

@Data
public class RegisterDeviceDTO {
    private Long userId;
    private String token; 
}