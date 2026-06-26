package local.wallet_service.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import local.wallet_service.dto.UserCreatedEvent;
import local.wallet_service.model.UserWallet;
import local.wallet_service.repository.UserWalletRepository;

import java.math.BigDecimal;

/**
 * Lắng nghe event từ user-service → tạo ví cho user mới.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreatedListener {

    private final UserWalletRepository userWalletRepo;

    @RabbitListener(queues = "user.created.queue")
    public void handleUserCreated(UserCreatedEvent e) {
        log.info("👤 [WalletService] Nhận event user.created: {}", e);

        try {
            // Nếu ví đã tồn tại thì bỏ qua
            if (userWalletRepo.findByUserId(e.getUserId()).isPresent()) {
                log.warn("⚠️ Ví đã tồn tại cho userId={}", e.getUserId());
                return;
            }

            // Tạo ví mới cho user
            UserWallet wallet = UserWallet.builder()
                    .userId(e.getUserId())
                    .balance(BigDecimal.ZERO)
                    .build();

            userWalletRepo.save(wallet);
            log.info("✅ [WalletService] Đã tạo ví mới cho userId={} (username={})", e.getUserId(), e.getUsername());
        } catch (Exception ex) {
            log.error("❌ [WalletService] Lỗi khi tạo ví cho userId={}: {}", e.getUserId(), ex.getMessage());
        }
    }
}
