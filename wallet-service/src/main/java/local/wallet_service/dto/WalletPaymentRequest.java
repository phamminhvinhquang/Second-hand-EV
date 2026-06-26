package local.wallet_service.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class WalletPaymentRequest {
    private Long userId;           // ID người dùng
    private BigDecimal amount;     // Số tiền thanh toán
    private String description;    // Mô tả giao dịch (VD: Thanh toán đơn hàng #123)
}
