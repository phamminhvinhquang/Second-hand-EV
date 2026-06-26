package local.wallet_service.dto;


import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStaffSalaryRequest {
    private BigDecimal salary;
    private Integer payDay;
    private String status;
}
