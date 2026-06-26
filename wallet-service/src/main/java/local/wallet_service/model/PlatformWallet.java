package local.wallet_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "platform_wallet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformWallet {

    @Id
    private Long id;

    @Column(precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
