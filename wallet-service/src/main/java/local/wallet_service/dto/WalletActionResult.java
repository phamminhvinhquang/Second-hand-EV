package local.wallet_service.dto;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletActionResult {
    private boolean success;
    private String message;
}
