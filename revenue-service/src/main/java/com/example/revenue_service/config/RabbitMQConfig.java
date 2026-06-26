// package com.example.revenue_service.config;

// import org.springframework.amqp.core.*;
// import org.springframework.amqp.rabbit.connection.ConnectionFactory;
// import org.springframework.amqp.rabbit.core.RabbitTemplate;
// import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// @Configuration
// public class RabbitMQConfig {

//     // Định nghĩa Exchange và Queue nội bộ để xử lý import
//     public static final String SYNC_EXCHANGE = "revenue.internal.exchange";
//     public static final String SYNC_QUEUE = "revenue.import.queue";
//     public static final String ROUTING_KEY = "revenue.import";

//     @Bean
//     public TopicExchange syncExchange() {
//         return new TopicExchange(SYNC_EXCHANGE, true, false);
//     }

//     @Bean
//     public Queue syncQueue() {
//         return new Queue(SYNC_QUEUE, true);
//     }

//     @Bean
//     public Binding binding(Queue syncQueue, TopicExchange syncExchange) {
//         return BindingBuilder.bind(syncQueue).to(syncExchange).with(ROUTING_KEY);
//     }

//     @Bean
//     public Jackson2JsonMessageConverter messageConverter() {
//         return new Jackson2JsonMessageConverter();
//     }

//     @Bean
//     public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//         RabbitTemplate template = new RabbitTemplate(connectionFactory);
//         template.setMessageConverter(messageConverter());
//         return template;
//     }
// }