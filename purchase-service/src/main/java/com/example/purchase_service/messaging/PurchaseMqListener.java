package com.example.purchase_service.messaging;

import com.example.purchase_service.model.Purchase;
import com.example.purchase_service.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseMqListener {

    private final PurchaseService purchaseService;

    /**
     * Lắng nghe routing key = "order.paid" trên exchange "ev.exchange".
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "purchase.order.paid.queue", durable = "true"),
            exchange = @Exchange(value = "${mq.exchange:ev.exchange}", type = ExchangeTypes.TOPIC, durable = "true"),
            key = "order.paid"
    ))
    public void onOrderPaid(Map<String, Object> payload) {
        try {
            log.info("📥 [MQ] Received order.paid event: {}", payload);

            // === 🛑 THAY ĐỔI QUAN TRỌNG ===
            // Lấy transactionId từ payload để kiểm tra trùng lặp
            String tx = payload.get("transactionId") != null ? String.valueOf(payload.get("transactionId")) : null;
            
            if (tx == null || tx.isBlank()) {
                log.warn("MQ order.paid missing transactionId, skipping");
                return;
            }

            // 1. Vẫn kiểm tra trùng lặp
            if (purchaseService.existsByTransactionId(tx)) {
                log.info("Purchase for transactionId={} already exists. Skipping.", tx);
                return;
            }

            // 2. Gọi hàm mới, truyền CẢ PAYLOAD (thay vì chỉ 'tx')
            //    Hàm này sẽ không gọi REST ngược lại transaction-service
            PurchaseCreationSafeFromEvent(payload);

        } catch (Exception e) {
            log.error("Error handling order.paid event", e);
        }
    }

    /**
     * Hàm helper mới: Tạo Purchase từ payload sự kiện MQ
     */
    private void PurchaseCreationSafeFromEvent(Map<String, Object> payload) {
        try {
            // Gọi hàm service mới, hàm này sẽ trích xuất sellerId, productId... từ payload
            purchaseService.createPurchaseFromEvent(payload);
            log.info("✅ Created Purchase from MQ event (txId={})", payload.get("transactionId"));
        } catch (Exception e) {
            log.error("Failed to create purchase from MQ event {}: {}", payload.get("transactionId"), e.getMessage(), e);
        }
    }

    /**
     * (Hàm cũ) - Vẫn giữ lại nhưng không dùng cho MQ nữa
     * Hàm này bây giờ chỉ được gọi bởi API (từ payment_success.js)
     */
    private void PurchaseCreationSafe(String tx) {
        try {
            purchaseService.createPurchaseFromTransaction(tx);
            log.info("✅ Created Purchase from transactionId={}", tx);
        } catch (Exception e) {
            log.error("Failed to create purchase from transaction {} : {}", tx, e.getMessage(), e);
        }
    }
}