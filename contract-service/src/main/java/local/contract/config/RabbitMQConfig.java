package local.contract.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ RabbitMQConfig cho contract-service
 * - Lắng nghe sự kiện "order.paid" từ transaction-service
 * - Dùng TopicExchange "ev.exchange" để đồng bộ với các service khác (wallet, user, transaction)
 */
@Configuration
public class RabbitMQConfig {

    // ======================= PROPERTIES =======================
    @Value("${mq.exchange:ev.exchange}")
    private String exchangeName;

    @Value("${mq.queue.order-paid:contract.order.paid.queue}")
    private String orderPaidQueueName;

    @Value("${mq.routing.order-paid:order.paid}")
    private String orderPaidRoutingKey;

    // ======================= EXCHANGE =======================
    @Bean
    public TopicExchange exchange() {
        // ⚙️ Dùng TopicExchange thay vì DirectExchange để đồng bộ hệ thống
        return new TopicExchange(exchangeName, true, false);
    }

    // ======================= QUEUE =======================
    @Bean
    public Queue orderPaidQueue() {
        // 🧾 Queue nhận event khi đơn hàng thanh toán thành công
        return QueueBuilder.durable(orderPaidQueueName).build();
    }

    // ======================= BINDING =======================
    @Bean
    public Binding bindingOrderPaid(Queue orderPaidQueue, TopicExchange exchange) {
        // 🪢 Gắn queue vào exchange với routing key "order.paid"
        return BindingBuilder.bind(orderPaidQueue)
                .to(exchange)
                .with(orderPaidRoutingKey);
    }

    // ======================= JSON CONVERTER =======================
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true); // giúp debug dễ hơn
        return converter;
    }

    // ======================= RABBIT TEMPLATE =======================
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
