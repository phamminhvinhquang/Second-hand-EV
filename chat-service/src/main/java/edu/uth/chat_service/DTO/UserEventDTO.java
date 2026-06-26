package edu.uth.chat_service.DTO;

import com.fasterxml.jackson.annotation.JsonAlias; // Nhớ import cái này
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEventDTO {
    
    //  @JsonAlias: Để nếu bên kia gửi "userId" thì nó vẫn map vào "id"
    @JsonAlias("userId") 
    private Long id;    
    
    private String name;
    private String email;
    private String phone;
}