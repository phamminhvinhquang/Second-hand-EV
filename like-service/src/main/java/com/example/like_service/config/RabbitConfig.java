package com.example.like_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // try both keys: rabbitmq.exchange OR app.rabbitmq.product-events.exchange
    @Value("${rabbitmq.exchange:${app.rabbitmq.product-events.exchange:product.events.exchange}}")
    private String exchangeName;

    @Value("${rabbitmq.queue:${app.rabbitmq.product-events.queue:product.update.queue}}")
    private String queueName;

    @Value("${rabbitmq.routingKey:${app.rabbitmq.product-events.routing-key:product.detail.updated}}")
    private String routingKey;

    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue productUpdateQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding productBinding(Queue productUpdateQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(productUpdateQueue).to(productExchange).with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                               Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(connectionFactory);
        f.setMessageConverter(converter);
        f.setConcurrentConsumers(1);
        f.setMaxConcurrentConsumers(4);
        return f;
    }
}
