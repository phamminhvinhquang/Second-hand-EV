// File: src/main/java/edu/uth/notification_service/Config/RabbitMQConfig.java
package edu.uth.notification_service.Config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName; // "ev.exchange"

    // 1. Config Notification
    @Value("${app.rabbitmq.queue}")
    private String queueName;
    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

   @Value("${app.rabbitmq.review.exchange}")
    private String reviewExchangeName;

    @Value("${app.rabbitmq.review.queue}")
    private String reviewQueueName;

    @Value("${app.rabbitmq.review.routing-key}")
    private String reviewRoutingKey;

   @Value("${app.rabbitmq.chat.queue}")
    private String chatQueueName;

    @Value("${app.rabbitmq.chat.routing-key}")
    private String chatRoutingKey;

    // --- Beans ---

    @Bean
    public TopicExchange listingExchange() {
        return new TopicExchange(exchangeName);
    }

    // Queue Notification
    @Bean
    public Queue notificationQueue() { return new Queue(queueName, true); }
    @Bean
    public Binding binding() {
        return BindingBuilder.bind(notificationQueue()).to(listingExchange()).with(routingKey);
    }

    @Bean
    public TopicExchange reviewExchange() { return new TopicExchange(reviewExchangeName); }

    @Bean
    public Queue reviewQueue() { return new Queue(reviewQueueName, true); }

    @Bean
    public Binding reviewBinding() {
        return BindingBuilder.bind(reviewQueue()).to(reviewExchange()).with(reviewRoutingKey);
    }

  @Bean
    public Queue chatQueue() { 
        // Sử dụng biến đã lấy từ file properties
        return new Queue(chatQueueName, true); 
    }
    
    @Bean
    public Binding chatBinding() {
        // Chat Service gửi vào Exchange chung (listingExchange)
        // Sử dụng biến routing key từ file properties
        return BindingBuilder.bind(chatQueue()).to(listingExchange()).with(chatRoutingKey);
    }
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(new ObjectMapper());
    }
}