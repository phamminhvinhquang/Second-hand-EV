package local.Second_hand_EV_Battery_Trading_Platform.model;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private List<Long> cartIds;     // ✅ danh sách giỏ hàng
    private Double totalAmount;     // tổng tiền sản phẩm
    private String paymentMethod;   // "vnpay" hoặc "momo"
    private CustomerDTO customer;   // thông tin khách hàng
    private String type;            // "order" hoặc "deposit" (nạp ví)
    private Long userId;            // id người nạp (dành cho deposit)
    private BigDecimal amount;      // ✅ số tiền nạp (hoặc tổng đơn)
}
