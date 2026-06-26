package local.wallet_service.dto;


import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaySalaryRequest {
    private Long userId;
    private BigDecimal amount;
    private String periodLabel;
}
