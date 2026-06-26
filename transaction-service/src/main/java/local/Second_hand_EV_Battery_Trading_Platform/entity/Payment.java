package local.Second_hand_EV_Battery_Trading_Platform.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity lưu thông tin thanh toán của người dùng
 * - Dùng cho cả order và nạp tiền ví
 * - Hỗ trợ các phương thức: VNPay, MoMo, EVWallet
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // ✅ Bổ sung để dùng Payment.builder()
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Liên kết với Customer (mỗi payment thuộc về 1 khách hàng)
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Mã giao dịch (UUID)
    @Column(name = "transaction_id", length = 100, unique = true)
    private String transactionId;

    // Phương thức thanh toán: VNPAY / MOMO / EVWALLET
    @Column(length = 50)
    private String method;

    // Số tiền thanh toán
    private BigDecimal amount;

    // Tổng tiền (sau khi cộng thêm phí / chiết khấu)
    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    // Trạng thái: PENDING / SUCCESS / FAILED
    @Column(length = 30)
    private String status;

    // Danh sách mã giỏ hàng liên quan (dành cho order)
    @ElementCollection
    @CollectionTable(name = "payment_cart_ids", joinColumns = @JoinColumn(name = "payment_id"))
    @Column(name = "cart_id")
    private List<Long> cartIdList;

    // Danh sách sản phẩm dạng text
    @Column(length = 1000)
    private String productNames;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "user_id")
    private Long userId;
}
