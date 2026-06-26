package local.contract.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSuccessEvent {
    private String transactionId;
    private BigDecimal amount;
    private String method;
    private String status;
    private Long userId;
    private Long sellerId;
    private String type; // "order" hoặc "deposit"
}
