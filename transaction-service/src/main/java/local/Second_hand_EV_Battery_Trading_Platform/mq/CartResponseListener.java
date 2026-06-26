package local.Second_hand_EV_Battery_Trading_Platform.mq;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CartResponseListener {

    private final Map<String, Map<String, Object>> responses = new ConcurrentHashMap<>();

    // Nhận phản hồi từ cart-service
    @RabbitListener(queues = "cart.fetch.response")
    public void handleCartResponse(Map<String, Object> data) {
        String transactionId = String.valueOf(data.get("transactionId"));
        responses.put(transactionId, data);
        log.info("📥 [MQ] Nhận phản hồi giỏ hàng từ cart-service cho transaction {}", transactionId);
    }

    // Lấy phản hồi theo transactionId (và xóa khỏi bộ nhớ cache)
    public Map<String, Object> getResponse(String transactionId) {
        return responses.remove(transactionId);
    }
}
