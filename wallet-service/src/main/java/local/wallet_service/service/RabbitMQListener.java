package local.wallet_service.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import local.wallet_service.dto.PaymentSuccessEvent;
import local.wallet_service.dto.WalletPaymentRequest;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQListener {

    private final WalletService walletService;

    // =========================================================
    // 🟢 NẠP TIỀN VÀO VÍ
    // =========================================================
    @RabbitListener(queues = "wallet.deposit.queue")
    public void handleWalletDeposit(@Payload PaymentSuccessEvent event) {
        try {
            if (event == null) {
                log.warn("⚠️ [WalletService] Nhận event NULL từ queue wallet.deposit.queue → bỏ qua");
                return;
            }

            log.info("📩 [WalletService] Nhận event wallet.deposit.success: {}", event);

            if (event.getUserId() == null || event.getPrice() == null) {
                log.warn("⚠️ [WalletService] Event thiếu thông tin: {}", event);
                return;
            }

            if (event.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("⚠️ [WalletService] Số tiền không hợp lệ: {}", event.getPrice());
                return;
            }

            if (walletService.isTransactionProcessed(event.getTransactionId(), event.getUserId())) {
                log.warn("⚠️ [WalletService] Giao dịch {} đã xử lý → bỏ qua", event.getTransactionId());
                return;
            }

            walletService.depositToUser(
                    event.getUserId(),
                    event.getPrice(),
                    event.getTransactionId(),
                    event.getMethod()
            );

            log.info("💰 [WalletService] Nạp thành công +{}đ vào ví userId={} (method={})",
                    event.getPrice(), event.getUserId(), event.getMethod());

        } catch (Exception e) {
            log.error("❌ [WalletService] Lỗi xử lý wallet.deposit.success: {}", e.getMessage(), e);
        }
    }

    // =========================================================
    // 🟠 TRỪ TỔNG TIỀN BUYER (EVWALLET)
    // =========================================================
    @RabbitListener(queues = "wallet.order.debit.queue")
    public void handleWalletOrderDebit(@Payload PaymentSuccessEvent event) {
        try {
            if (event == null) {
                log.warn("⚠️ [WalletService] Nhận event NULL từ queue wallet.order.debit.queue → bỏ qua");
                return;
            }

            log.info("📩 [WalletService] Nhận event wallet.order.debit: {}", event);

            if (event.getUserId() == null || event.getPrice() == null) {
                log.warn("⚠️ [WalletService] Event thiếu userId hoặc price: {}", event);
                return;
            }

            if (walletService.isTransactionProcessed(event.getTransactionId(), event.getUserId())) {
                log.warn("⚠️ [WalletService] Giao dịch {} đã được xử lý → bỏ qua", event.getTransactionId());
                return;
            }

            WalletPaymentRequest req = new WalletPaymentRequest();
            req.setUserId(event.getUserId());
            req.setAmount(event.getPrice()); // nội bộ WalletPaymentRequest vẫn dùng amount
            req.setDescription("Tổng thanh toán đơn hàng #" + event.getTransactionId());
            walletService.payWithWallet(req);

            log.info("💳 [WalletService] Đã trừ {}đ khỏi ví người mua #{}", event.getPrice(), event.getUserId());

        } catch (Exception e) {
            log.error("❌ [WalletService] Lỗi xử lý wallet.order.debit: {}", e.getMessage(), e);
        }
    }

    // =========================================================
    // 🟣 CHIA HOA HỒNG CHO SELLER & PLATFORM
    // =========================================================
    @RabbitListener(queues = "wallet.order.paid.queue")
    public void handleOrderPaid(@Payload PaymentSuccessEvent event) {
        try {
            if (event == null) {
                log.warn("⚠️ [WalletService] Nhận event NULL từ order.paid.queue → bỏ qua");
                return;
            }

            log.info("📩 [WalletService] Nhận event order.paid: {}", event);

            if (event.getPrice() == null || event.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("⚠️ [WalletService] Số tiền không hợp lệ → {}", event);
                return;
            }

            if (event.getSellerId() == null || event.getSellerId() <= 0) {
                log.warn("⚠️ [WalletService] sellerId trống hoặc -1 → {}", event);
                return;
            }

            // ⚙️ Kiểm tra trùng bằng bảng commission_record thay vì transaction
            boolean alreadyPaid = walletService.hasCommissionRecord(
                    event.getTransactionId(),
                    event.getSellerId(),
                    event.getProductId()
            );
            if (alreadyPaid) {
                log.warn("⚠️ [WalletService] Seller #{} đã nhận hoa hồng cho productId={} (txId={}) → bỏ qua",
                        event.getSellerId(), event.getProductId(), event.getTransactionId());
                return;
            }


            String result = walletService.applyCommission(event);
            log.info("✅ [WalletService] Commission result: {}", result);

        } catch (Exception e) {
            log.error("❌ [WalletService] Lỗi khi xử lý order.paid: {}", e.getMessage(), e);
        }
    }
}
