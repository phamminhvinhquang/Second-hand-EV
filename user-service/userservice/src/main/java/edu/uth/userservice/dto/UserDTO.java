// File: edu.uth.userservice.dto.UserDTO.java
package edu.uth.userservice.dto;

import edu.uth.userservice.model.User;
import edu.uth.userservice.model.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class UserDTO {

    private Integer id; 
    private String name;
    private String email;
    private String phone;
    private String address;
    private String accountStatus;
    private Set<String> roles; 

    public UserDTO(User user) {
        this.id = user.getUserId(); 
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.address = user.getAddress();
        this.accountStatus = user.getAccountStatus();
        
        if (user.getRoles() != null) {
            this.roles = user.getRoles().stream()
                               .map(Role::getName)
                               .collect(Collectors.toSet());
        }
    }
} // <-- Đảm bảo file của bạn kết thúc tại đây