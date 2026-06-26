package local.Second_hand_EV_Battery_Trading_Platform.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String status;
    private String message;
    private String transactionId;
    private String redirectUrl;
}

