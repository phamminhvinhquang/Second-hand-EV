package edu.uth.userservice.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MQPublisher {

    private final RabbitTemplate rabbitTemplate;

    // Cho phép cấu hình qua application.yml, mặc định là "ev.exchange"
    @Value("${mq.exchange:ev.exchange}")
    private String exchange;

    /**
     * 📤 Gửi message JSON tới RabbitMQ
     * @param routingKey  Routing key (VD: "user.created", "user.role.updated")
     * @param payload     Object (Java Bean hoặc Map) sẽ tự động convert sang JSON
     */
    public void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.info("📤 [MQPublisher] Sent → Exchange='{}', Key='{}', Payload={}", exchange, routingKey, payload);
        } catch (Exception e) {
            log.error("❌ [MQPublisher] Failed to send message: {}", e.getMessage(), e);
        }
    }
}
