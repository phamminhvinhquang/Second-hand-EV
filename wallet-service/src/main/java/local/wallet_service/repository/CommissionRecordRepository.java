package local.wallet_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import local.wallet_service.model.CommissionRecord;

public interface CommissionRecordRepository extends JpaRepository<CommissionRecord, Long> {
    boolean existsByTransactionIdAndSellerId(String transactionId, Long sellerId);
    boolean existsByTransactionIdAndSellerIdAndProductId(String transactionId, Long sellerId, Long productId);


}
