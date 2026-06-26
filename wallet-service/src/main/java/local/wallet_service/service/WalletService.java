package local.wallet_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import local.wallet_service.dto.PaymentSuccessEvent;
import local.wallet_service.dto.WalletPaymentRequest;
import local.wallet_service.model.*;
import local.wallet_service.model.enums.*;
import local.wallet_service.repository.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserWalletRepository userWalletRepo;
    private final PlatformWalletRepository platformRepo;
    private final WalletTransactionRepository walletTxRepo;
    private final CommissionRecordRepository commissionRepo;

    @Value("${wallet.platform.id:1}")
    private Long platformWalletId;

    // ============================================================
    // ✅ 1️⃣ CHỐNG TRÙNG GIAO DỊCH
    // ============================================================
    @Transactional(readOnly = true)
    public boolean isTransactionProcessed(String txId, Long walletRefId) {
        if (txId == null || txId.isBlank()) return false;

        if (walletRefId != null) {
            return walletTxRepo.existsByDescriptionContainingAndWalletRefId(txId, walletRefId);
        }

        return walletTxRepo.existsByDescriptionContaining(txId + " [TOTAL]");
    }

    // ============================================================
    // ✅ 2️⃣ ÁP DỤNG HOA HỒNG SAU THANH TOÁN
    // ============================================================
    @Transactional
    public String applyCommission(PaymentSuccessEvent e) {
        if (e == null || e.getSellerId() == null || e.getPrice() == null) {
            return "❌ Invalid event payload";
        }

        // ⚠️ Buyer == Seller → bỏ qua
        if (e.getUserId() != null && e.getUserId().equals(e.getSellerId())) {
            log.warn("⚠️ [Commission] Buyer #{}, Seller #{} là cùng 1 người → bỏ qua hoa hồng", e.getUserId(), e.getSellerId());
            return "⚠️ Skip self-transaction commission";
        }

        BigDecimal amount = e.getPrice(); // ✅ đổi từ getAmount() sang getPrice()
        Long sellerId = e.getSellerId();
        String txId = e.getTransactionId();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "❌ Invalid amount: " + amount;
        }

        if (commissionRepo.existsByTransactionIdAndSellerIdAndProductId(txId, sellerId, e.getProductId())) {
                log.warn("⚠️ [Commission] Seller #{} already got commission for txId {} & productId {} → skip",
                        sellerId, txId, e.getProductId());
                return "⚠️ Duplicate commission record for seller #" + sellerId + " (productId=" + e.getProductId() + ")";
        }


        // 🔹 10% cho sàn – 90% cho seller
        BigDecimal commission = amount.multiply(BigDecimal.valueOf(0.10));
        BigDecimal sellerIncome = amount.subtract(commission);

        // --- Cập nhật ví sàn ---
        PlatformWallet platform = platformRepo.findById(platformWalletId)
                .orElseGet(() -> platformRepo.save(
                        PlatformWallet.builder()
                                .id(platformWalletId)
                                .balance(BigDecimal.ZERO)
                                .build()
                ));

        platform.setBalance(platform.getBalance().add(commission));
        platformRepo.save(platform);

        walletTxRepo.save(WalletTransaction.builder()
                .walletType(WalletType.PLATFORM)
                .walletRefId(platformWalletId)
                .txType(TxType.CREDIT)
                .amount(commission)
                .description("Commission 10% from order #" + txId + " [COMMISSION] seller#" + sellerId)
                                .build());

        // --- Cập nhật ví người bán ---
        UserWallet sellerWallet = userWalletRepo.findByUserId(sellerId)
                .orElseGet(() -> userWalletRepo.save(
                        UserWallet.builder()
                                .userId(sellerId)
                                .balance(BigDecimal.ZERO)
                                .build()
                ));

        sellerWallet.setBalance(sellerWallet.getBalance().add(sellerIncome));
        userWalletRepo.save(sellerWallet);

        walletTxRepo.save(WalletTransaction.builder()
                .walletType(WalletType.USER)
                .walletRefId(sellerId)
                .txType(TxType.CREDIT)
                .amount(sellerIncome)
                .description("Seller income (90%) from order #" + txId + " [COMMISSION] seller#" + sellerId)
                                .build());

        // --- Ghi record hoa hồng ---
        commissionRepo.save(CommissionRecord.builder()
                .transactionId(txId)
                .sellerId(sellerId)
                .productId(e.getProductId()) // 🆕 Thêm
                .amount(commission)
                .status(RecordStatus.PAID)
                .build());

        log.info("💰 [Commission] +{} to platform, +{} to seller #{} (txId={})",
                commission, sellerIncome, sellerId, txId);

        return "✅ Commission processed successfully";
    }

    // ============================================================
    // ✅ 3️⃣ NẠP TIỀN VÀO VÍ NGƯỜI DÙNG
    // ============================================================
    @Transactional
    public String depositToUser(Long userId, BigDecimal amount, String transactionId, String method) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "❌ Invalid deposit data";
        }

        if (isTransactionProcessed(transactionId, null)) {
            log.warn("⚠️ [TransferToSeller] Transaction {} already processed → skip transfer.", transactionId);
            return "⚠️ Transfer skipped (duplicate transaction)";
        }

        UserWallet wallet = userWalletRepo.findByUserId(userId)
                .orElseGet(() -> userWalletRepo.save(
                        UserWallet.builder()
                                .userId(userId)
                                .balance(BigDecimal.ZERO)
                                .build()
                ));

        wallet.setBalance(wallet.getBalance().add(amount));
        userWalletRepo.save(wallet);

        walletTxRepo.save(WalletTransaction.builder()
                .walletType(WalletType.USER)
                .walletRefId(userId)
                .txType(TxType.CREDIT)
                .amount(amount)
                .description("Deposit via " + method + " (txId=" + transactionId + ")")
                                .build());

        log.info("💵 [Deposit] +{} to user #{} via {} (txId={})", amount, userId, method, transactionId);
        return "✅ Deposit " + amount + " added successfully to user wallet";
    }

    // ============================================================
    // ✅ 4️⃣ LẤY SỐ DƯ & LỊCH SỬ
    // ============================================================
    @Transactional(readOnly = true)
    public BigDecimal getUserBalance(Long userId) {
        return userWalletRepo.findByUserId(userId)
                .map(UserWallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public BigDecimal getPlatformBalance() {
        return platformRepo.findById(platformWalletId)
                .map(PlatformWallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> getUserTransactions(Long userId) {
        return walletTxRepo.findByWalletTypeAndWalletRefIdOrderByCreatedAtDesc(WalletType.USER, userId);
    }

    // ============================================================
    // ✅ 5️⃣ THANH TOÁN BẰNG VÍ EV
    // ============================================================
    @Transactional
    public String payWithWallet(WalletPaymentRequest req) {
        if (req.getUserId() == null || req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return "❌ Dữ liệu thanh toán không hợp lệ";
        }

        String txId = req.getDescription() != null && req.getDescription().contains("#")
                ? req.getDescription().split("#")[1].trim()
                : req.getDescription();

        if (isTransactionProcessed(txId, null)) {
            log.warn("⚠️ [EV Wallet] Giao dịch {} đã được xử lý → bỏ qua", txId);
            return "⚠️ Payment skipped (duplicate transaction)";
        }

        UserWallet wallet = userWalletRepo.findByUserId(req.getUserId())
                .orElseGet(() -> userWalletRepo.save(
                        UserWallet.builder()
                                .userId(req.getUserId())
                                .balance(BigDecimal.ZERO)
                                .build()
                ));

        if (wallet.getBalance().compareTo(req.getAmount()) < 0) {
            return "❌ Số dư không đủ để thanh toán";
        }

        wallet.setBalance(wallet.getBalance().subtract(req.getAmount()));
        userWalletRepo.save(wallet);

        walletTxRepo.save(WalletTransaction.builder()
                .walletType(WalletType.USER)
                .walletRefId(req.getUserId())
                .txType(TxType.DEBIT)
                .amount(req.getAmount())
                .description("Thanh toán tổng đơn hàng (txId=" + txId + ") [TOTAL]")
                                .build());

        log.info("💳 [EV Wallet] -{} from user #{} (txId={})", req.getAmount(), req.getUserId(), txId);

        return "✅ Thanh toán bằng ví EV thành công. Đã trừ " + req.getAmount() + "đ";
    }

    // ============================================================
    // ✅ 6️⃣ CHUYỂN TIỀN CHO SELLER
    // ============================================================
    @Transactional
    public String transferToSeller(Long sellerId, BigDecimal amount, String transactionId, String method, Long buyerId) {
        if (sellerId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "❌ Invalid seller transfer data";
        }

        if (buyerId != null && buyerId.equals(sellerId)) {
            log.warn("⚠️ [TransferToSeller] Buyer #{} == Seller #{} → bỏ qua tự giao dịch", buyerId, sellerId);
            return "⚠️ Skip self-transfer (buyer == seller)";
        }

        if (isTransactionProcessed(transactionId, sellerId)) {
            log.warn("⚠️ [TransferToSeller] Transaction {} for seller #{} already processed → skip.", transactionId, sellerId);
            return "⚠️ Transfer skipped for seller #" + sellerId;
        }

        UserWallet sellerWallet = userWalletRepo.findByUserId(sellerId)
                .orElseGet(() -> userWalletRepo.save(
                        UserWallet.builder().userId(sellerId).balance(BigDecimal.ZERO).build()
                ));
        sellerWallet.setBalance(sellerWallet.getBalance().add(amount));
        userWalletRepo.save(sellerWallet);

        walletTxRepo.save(WalletTransaction.builder()
                .walletType(WalletType.USER)
                .walletRefId(sellerId)
                .txType(TxType.CREDIT)
                .amount(amount)
                .description("Seller received " + amount + " via " + method +
                        " (buyerId=" + buyerId + ", txId=" + transactionId +
                        ") [SELLER#" + sellerId + "]")
                                .build());

        log.info("💸 [TransferToSeller] +{}đ to seller #{} (method={}, buyerId={}, txId={})",
                amount, sellerId, method, buyerId, transactionId);

        return "✅ Seller transfer completed successfully";
    }

    // ============================================================
    // ✅ 7️⃣ HỖ TRỢ NẠP / CHIA PHÍ
    // ============================================================
    @Transactional
    public void addToPlatformWallet(BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("⚠️ [PlatformWallet] Số tiền không hợp lệ → {}", amount);
            return;
        }

        PlatformWallet platformWallet = platformRepo.findById(platformWalletId)
                .orElseGet(() -> platformRepo.save(
                        PlatformWallet.builder()
                                .id(platformWalletId)
                                .balance(BigDecimal.ZERO)
                                .build()
                ));

        platformWallet.setBalance(platformWallet.getBalance().add(amount));
        platformRepo.save(platformWallet);

        walletTxRepo.save(WalletTransaction.builder()
                .walletType(WalletType.PLATFORM)
                .walletRefId(platformWallet.getId())
                .txType(TxType.CREDIT)
                .amount(amount)
                .description(description)
                                .build());

        log.info("🏦 [PlatformWallet] +{} vào ví sàn (desc='{}')", amount, description);
    }

    @Transactional
    public void addToUserWallet(Long userId, BigDecimal amount, String description) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("⚠️ [UserWallet] Dữ liệu không hợp lệ → userId={}, amount={}", userId, amount);
            return;
        }

        UserWallet wallet = userWalletRepo.findByUserId(userId)
                .orElseGet(() -> userWalletRepo.save(
                        UserWallet.builder()
                                .userId(userId)
                                .balance(BigDecimal.ZERO)
                                .build()
                ));

        wallet.setBalance(wallet.getBalance().add(amount));
        userWalletRepo.save(wallet);

        walletTxRepo.save(WalletTransaction.builder()
                .walletType(WalletType.USER)
                .walletRefId(userId)
                .txType(TxType.CREDIT)
                .amount(amount)
                .description(description)
                                .build());

        log.info("💼 [UserWallet] +{} vào ví người dùng #{} (desc='{}')", amount, userId, description);
    }

    // ============================================================
    // ✅ 8️⃣ LỊCH SỬ GIAO DỊCH
    // ============================================================
    @Transactional(readOnly = true)
    public List<WalletTransaction> getPlatformTransactions() {
        return walletTxRepo.findByWalletTypeAndWalletRefIdOrderByCreatedAtDesc(WalletType.PLATFORM, platformWalletId);
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> getAllTransactions() {
        return walletTxRepo.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

        @Transactional(readOnly = true)
                public boolean hasCommissionRecord(String txId, Long sellerId, Long productId) {
                return commissionRepo.existsByTransactionIdAndSellerIdAndProductId(txId, sellerId, productId);
        }


        @Transactional(readOnly = true)
        public BigDecimal getBalanceByUserId(Long userId) {
                UserWallet wallet = userWalletRepo.findByUserId(userId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của userId=" + userId));
                return wallet.getBalance();
        }

}
