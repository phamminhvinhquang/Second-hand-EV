package local.wallet_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import local.wallet_service.model.enums.*;
import java.time.ZoneId;

@Entity
@Table(name = "wallet_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private WalletType walletType;

    private Long walletRefId;

    @Enumerated(EnumType.STRING)
    private TxType txType;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ✅ Tự động set thời gian khi insert
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }
}
