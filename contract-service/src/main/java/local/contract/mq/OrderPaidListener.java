package local.contract.mq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import local.contract.model.ContractRequest;
import local.contract.model.PaymentSuccessEvent;
import local.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidListener {

    private final ContractService contractService;

    /**
     * 🧾 Lắng nghe sự kiện "order.paid.queue"
     * - Event này được publish từ transaction-service (routingKey = "order.paid")
     * - Được định tuyến qua TopicExchange "ev.exchange"
     */
    @RabbitListener(queues = "${mq.queue.order-paid:contract.order.paid.queue}")
    public void handleOrderPaidEvent(@Payload PaymentSuccessEvent event) {
        try {
            log.info("📥 [MQ] Nhận PaymentSuccessEvent: {}", event);

            // 🧩 Kiểm tra dữ liệu hợp lệ
            if (event == null || event.getTransactionId() == null) {
                log.warn("⚠️ [Contract] Nhận event null hoặc thiếu transactionId → bỏ qua");
                return;
            }

            // 🔹 Chỉ xử lý nếu type là "order"
            if (event.getType() != null && !event.getType().equalsIgnoreCase("order")) {
                log.info("⏭️ [Contract] Bỏ qua event type={} (không phải đơn hàng)", event.getType());
                return;
            }

            // ✅ Tạo ContractRequest từ event nhận được
            ContractRequest req = new ContractRequest();
            req.setTransactionId(event.getTransactionId());
            req.setMethod(event.getMethod());
            req.setUserId(event.getUserId());
            req.setSellerId(event.getSellerId());

            contractService.createContract(req);
            log.info("✅ [Contract] Đã tạo hợp đồng cho transactionId={}", event.getTransactionId());

        } catch (Exception e) {
            log.error("❌ [Contract] Lỗi khi xử lý PaymentSuccessEvent: {}", e.getMessage(), e);
        }
    }
}
