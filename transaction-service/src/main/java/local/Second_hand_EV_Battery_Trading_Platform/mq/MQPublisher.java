package local.Second_hand_EV_Battery_Trading_Platform.mq;

import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ✅ MQPublisher
 * - Gửi message tới RabbitMQ exchange "ev.exchange"
 * - Đơn giản, an toàn vì callback đã được cấu hình trong RabbitMQConfig.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MQPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${mq.exchange:ev.exchange}")
    private String exchange;

    /**
     * ✅ Gửi message có log chi tiết
     */
    public void publish(String routingKey, Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.info("📤 [MQPublisher] Sent event: {} | Payload: {}", routingKey, payload);
        } catch (Exception e) {
            log.error("❌ [MQPublisher] Error sending message to MQ: {}", e.getMessage(), e);
        }
    }
}
