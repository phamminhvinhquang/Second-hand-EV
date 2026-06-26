package local.contract.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import local.contract.entity.Contract;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    List<Contract> findByUserId(Long userId);
}
