package local.wallet_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StaffSalaryDTO {
    private Long userId;
    private BigDecimal salary;
    private Integer payDay;
    private String status;
    private LocalDate lastPaid;
}
