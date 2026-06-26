package local.wallet_service.model;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_wallet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
