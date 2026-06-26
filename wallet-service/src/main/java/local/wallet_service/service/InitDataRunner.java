package local.wallet_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import local.wallet_service.model.PlatformWallet;
import local.wallet_service.model.WalletTransaction;
import local.wallet_service.model.enums.WalletType;
import local.wallet_service.model.enums.TxType;
import local.wallet_service.repository.PlatformWalletRepository;
import local.wallet_service.repository.WalletTransactionRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitDataRunner implements CommandLineRunner {

    private final PlatformWalletRepository platformRepo;
    private final WalletTransactionRepository transactionRepo;

    @Value("${wallet.platform.id:1}")
    private Long platformWalletId;

    @Override
    public void run(String... args) {

        platformRepo.findById(platformWalletId).ifPresentOrElse(
            wallet -> {
                log.info("ℹ️ Ví sàn đã tồn tại → Số dư hiện tại: {}đ", wallet.getBalance());
            },
            () -> {
                // 🟢 Tạo ví sàn lần đầu
                PlatformWallet wallet = PlatformWallet.builder()
                        .id(platformWalletId)
                        .balance(new BigDecimal("500000000")) // 500 triệu
                        .build();
                platformRepo.save(wallet);

                log.info("🎉 Đã tạo ví sàn mới với số dư 500,000,000đ");

                // 🟢 Tạo transaction để UI hiển thị
                WalletTransaction tx = WalletTransaction.builder()
                        .walletType(WalletType.PLATFORM)   // ⭐ ENUM
                        .walletRefId(platformWalletId)
                        .txType(TxType.CREDIT)              // ⭐ ENUM
                        .amount(new BigDecimal("500000000"))
                        .description("Initial platform funding")
                        .createdAt(LocalDateTime.now())
                        .build();

                transactionRepo.save(tx);

                log.info("🧾 Đã tạo transaction khởi tạo ví sàn");
            }
        );
    }
}
