package local.wallet_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;
import local.wallet_service.model.enums.RecordStatus;

@Entity
@Table(name = "commission_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 ID giao dịch (transactionId từ transaction-service)
    private String transactionId;

    // 🔹 ID người bán nhận tiền
    private Long sellerId;

    @Column(name = "product_id")
    private Long productId;
    
    // 🔹 Số tiền hoa hồng (10%)
    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    // 🔹 Trạng thái bản ghi
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RecordStatus status = RecordStatus.PENDING;

    // 🔹 Thời gian tạo bản ghi
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
