package local.wallet_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import local.wallet_service.model.PlatformWallet;

public interface PlatformWalletRepository extends JpaRepository<PlatformWallet, Long> {}

