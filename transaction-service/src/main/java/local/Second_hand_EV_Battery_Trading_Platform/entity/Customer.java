package local.Second_hand_EV_Battery_Trading_Platform.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity lưu thông tin khách hàng thanh toán
 * Dùng cho cả giao dịch mua hàng và nạp ví
 */
@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // ✅ Cho phép dùng Customer.builder()
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
