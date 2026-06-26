package local.wallet_service.model;


import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staff_salary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(precision = 18, scale = 2)
    private BigDecimal salary;

    private Integer payDay; // ngày trả (1-28)
    private String status;  // ACTIVE / INACTIVE
    private LocalDate startDate; // ✅ ngày bắt đầu trở thành nhân viên
    private LocalDate lastPaid;
}
