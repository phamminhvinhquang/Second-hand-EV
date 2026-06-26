// cart-service/src/main/java/com/example/cart_service/config/RabbitMQConfig.java
package com.example.cart_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    // Lấy tên exchange từ application.properties
    @Value("${app.rabbitmq.product-events.exchange}")
    private String productEventsExchange;

    // === 1️⃣ Queue đồng bộ sản phẩm (từ product-service) ===
    public static final String QUEUE_NAME = "cart.product.sync.queue";
    public static final String ROUTING_KEY = "product.#";

    @Bean
    public Queue cartSyncQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public TopicExchange productEventsExchange() {
        return new TopicExchange(productEventsExchange, true, false);
    }

    @Bean
    public Binding cartSyncBinding(Queue cartSyncQueue, TopicExchange productEventsExchange) {
        return BindingBuilder.bind(cartSyncQueue).to(productEventsExchange).with(ROUTING_KEY);
    }

    // === 2️⃣ Converter và template ===
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }

    // === 3️⃣ Exchange chung giữa các service (transaction, cart, wallet, ...) ===
    @Bean
    public TopicExchange evExchange() {
        return new TopicExchange("ev.exchange", true, false);
    }

    // === 4️⃣ Queue lấy giỏ hàng từ transaction-service ===
    @Bean
    public Queue cartFetchRequestQueue() {
        return new Queue("cart.fetch.request", true);
    }

    @Bean
    public Queue cartFetchResponseQueue() {
        return new Queue("cart.fetch.response", true);
    }

    @Bean
    public Binding bindCartFetchRequest(Queue cartFetchRequestQueue, TopicExchange evExchange) {
        return BindingBuilder.bind(cartFetchRequestQueue)
                .to(evExchange)
                .with("cart.fetch.request");
    }

    @Bean
    public Binding bindCartFetchResponse(Queue cartFetchResponseQueue, TopicExchange evExchange) {
        return BindingBuilder.bind(cartFetchResponseQueue)
                .to(evExchange)
                .with("cart.fetch.response");
    }

    // === 5️⃣ Queue để nhận event thanh toán thành công (từ transaction-service) ===
    @Bean
    public Queue cartOrderPaidQueue() {
        return new Queue("cart.order.paid", true);
    }

    @Bean
    public Binding bindCartOrderPaidQueue(Queue cartOrderPaidQueue, TopicExchange evExchange) {
        return BindingBuilder.bind(cartOrderPaidQueue)
                .to(evExchange)
                .with("cart.order.paid");
    }
}
