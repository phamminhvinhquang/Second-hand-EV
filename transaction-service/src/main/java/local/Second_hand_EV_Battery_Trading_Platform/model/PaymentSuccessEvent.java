package local.Second_hand_EV_Battery_Trading_Platform.model;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ✅ PaymentSuccessEvent
 * DTO dùng để truyền dữ liệu giao dịch thành công giữa transaction-service ↔ wallet-service qua RabbitMQ.
 * Phải đồng bộ 1:1 với class cùng tên trong wallet-service để tránh lỗi deserialize JSON.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentSuccessEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Mã giao dịch duy nhất */
    private String transactionId;

    /** ID người bán */
    private Long sellerId;

    /** Số tiền thanh toán */
    private BigDecimal amount;

    /** Phương thức thanh toán: MOMO / VNPAY / EVWALLET */
    private String method;

    /** ID người mua (người thực hiện thanh toán) */
    private Long userId;

    /** Trạng thái giao dịch: SUCCESS / FAILED */
    private String status;

    /** Loại giao dịch: order / deposit / withdraw */
    private String type;
}
