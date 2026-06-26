package local.Second_hand_EV_Battery_Trading_Platform.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * ✅ RabbitMQConfig cho transaction-service (Publisher only)
 * ---------------------------------------------------------
 * - Chỉ khai báo TopicExchange "ev.exchange".
 * - KHÔNG tạo queue hoặc binding (để tránh chia tải với service khác).
 * - Có ConfirmCallback & ReturnsCallback để bảo đảm message không mất.
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "ev.exchange";

    // ======================= EXCHANGE =======================
    @Bean
    public TopicExchange exchange() {
        // Exchange trung tâm cho toàn hệ thống
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    // ======================= CONVERTER =======================
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ======================= RABBIT TEMPLATE =======================
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true); // kích hoạt ReturnsCallback

        // ConfirmCallback: xác nhận gửi thành công đến exchange
        template.setConfirmCallback((correlation, ack, cause) -> {
            if (ack) {
                log.debug("✅ [RabbitMQ] Message confirmed to exchange '{}'", EXCHANGE_NAME);
            } else {
                log.error("❌ [RabbitMQ] Failed to deliver message to exchange '{}': {}", EXCHANGE_NAME, cause);
            }
        });

        // ReturnsCallback: khi message không được route đến queue nào
        template.setReturnsCallback(returned -> log.error(
            "⚠️ [RabbitMQ] Message returned: replyCode={} replyText={} routingKey={} body={}",
            returned.getReplyCode(),
            returned.getReplyText(),
            returned.getRoutingKey(),
            new String(returned.getMessage().getBody())
        ));

        return template;
    }
}
