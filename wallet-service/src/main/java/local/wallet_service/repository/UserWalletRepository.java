package local.wallet_service.repository;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import local.wallet_service.model.UserWallet;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {
    Optional<UserWallet> findByUserId(Long userId);
}


