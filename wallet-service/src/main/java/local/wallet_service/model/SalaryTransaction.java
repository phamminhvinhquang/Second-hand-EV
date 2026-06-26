package local.wallet_service.model;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import local.wallet_service.model.enums.RecordStatus;

@Entity
@Table(name = "salary_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    private String periodLabel; // ví dụ: "2025-11"
    private String note;

    @Enumerated(EnumType.STRING)
    private RecordStatus status;

    private LocalDateTime payDate = LocalDateTime.now();
}
