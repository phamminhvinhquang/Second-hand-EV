package edu.uth.userservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ RabbitMQConfig cho user-service
 * - Dùng TopicExchange "ev.exchange" để đồng bộ với transaction-service & wallet-service.
 * - Publish các event như user.created, user.role.updated ...
 */
@Configuration
public class RabbitMQConfig {
 public static final String ASYNC_REQUEST_KEY = "listing.created.needs_user_data";
    public static final String ASYNC_REQUEST_QUEUE = "listing.needs_user_data.queue";
    public static final String ASYNC_RESPONSE_KEY = "user.info.found";
    public static final String EXCHANGE_NAME = "ev.exchange";

    // ===================== EXCHANGE CHÍNH =====================
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    // ===================== QUEUE: user.created =====================
    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable("user.created.queue").build();
    }

    @Bean
    public Binding bindUserCreated(Queue userCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(userCreatedQueue).to(exchange).with("user.created");
    }

    // ===================== QUEUE: user.role.updated =====================
    @Bean
    public Queue userRoleUpdatedQueue() {
        return QueueBuilder.durable("user.role.updated.queue").build();
    }

    @Bean
    public Binding bindUserRoleUpdated(Queue userRoleUpdatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(userRoleUpdatedQueue).to(exchange).with("user.role.updated");
    }
    // ===================== QUEUE BẤT ĐỒNG BỘ MỚI (NHẬN YÊU CẦU) =====================
    
    @Bean
    public Queue asyncListingRequestQueue() {
        // Queue này nhận yêu cầu từ ListingService
        return QueueBuilder.durable(ASYNC_REQUEST_QUEUE).build();
    }

    @Bean
    public Binding bindAsyncListingRequest(Queue asyncListingRequestQueue, TopicExchange exchange) {
        // Gắn queue này vào exchange chung với routing key "listing.created.needs_user_data"
        return BindingBuilder.bind(asyncListingRequestQueue).to(exchange).with(ASYNC_REQUEST_KEY);
    }

    // ===================== JSON Converter =====================
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ===================== RabbitTemplate (publish) =====================
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
    // === ⭐️ BỔ SUNG LẮNG NGHE LỊCH SỬ GIAO DỊCH ===

// 1. Queue nhận sự kiện MUA HÀNG
@Bean
public Queue historyOrderPaidQueue() {
    // Tên queue "user.history.order_paid"
    return new Queue("user.history.order_paid", true);
}

// 2. Queue nhận sự kiện NẠP TIỀN
@Bean
public Queue historyDepositQueue() {
    // Tên queue "user.history.deposit_success"
    return new Queue("user.history.deposit_success", true);
}

// 3. Gắn (bind) queue MUA HÀNG vào exchange
@Bean
public Binding bindingOrderPaid(Queue historyOrderPaidQueue, TopicExchange exchange) {
    // Lắng nghe key "order.paid"
    return BindingBuilder.bind(historyOrderPaidQueue).to(exchange).with("order.paid");
}

// 4. Gắn (bind) queue NẠP TIỀN vào exchange
@Bean
public Binding bindingDepositSuccess(Queue historyDepositQueue, TopicExchange exchange) {
    // Lắng nghe key "wallet.deposit.success"
    return BindingBuilder.bind(historyDepositQueue).to(exchange).with("wallet.deposit.success");
}
}
