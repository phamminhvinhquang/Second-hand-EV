package local.wallet_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import local.wallet_service.model.SalaryTransaction;

public interface SalaryTransactionRepository extends JpaRepository<SalaryTransaction, Long> {
    List<SalaryTransaction> findByUserIdOrderByPayDateDesc(Long userId);
}
