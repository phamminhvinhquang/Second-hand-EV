package local.wallet_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import local.wallet_service.model.WalletTransaction;
import local.wallet_service.model.enums.WalletType;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // 🔹 Lấy lịch sử giao dịch theo loại ví & id ví
    List<WalletTransaction> findByWalletTypeAndWalletRefIdOrderByCreatedAtDesc(WalletType walletType, Long walletRefId);

    // 🔹 Kiểm tra giao dịch đã tồn tại dựa trên txId trong phần mô tả


    boolean existsByDescriptionContainingAndWalletRefId(String description, Long walletRefId);
    boolean existsByDescriptionContaining(String description);

}
