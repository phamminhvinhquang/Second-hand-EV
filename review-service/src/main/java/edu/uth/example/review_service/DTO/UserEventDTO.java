
package edu.uth.example.review_service.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO này chứa thông tin người dùng được gửi qua RabbitMQ từ User Service
 * để đồng bộ hóa.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class UserEventDTO {

    /**
     * Map trường "userId" (từ JSON của User-Service)
     * vào trường "id" của DTO này.
     */
    @JsonProperty("userId")
    private Integer id;

    private String name;

}