package local.wallet_service.dto;

import lombok.*;

/**
 * Dữ liệu được nhận từ user-service khi user mới được tạo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent {
    private Long userId;
    private String username;
    private String role;
}
