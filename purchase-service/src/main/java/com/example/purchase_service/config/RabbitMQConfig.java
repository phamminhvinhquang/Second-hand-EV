package com.example.purchase_service.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * RabbitMQ configuration (consumer + template).
 *
 * - RabbitTemplate: send JSON (Jackson)
 * - RabbitListenerContainerFactory: use ContentTypeDelegatingMessageConverter so we can accept
 *   both application/json and application/x-java-serialized-object (but only allowed classes).
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate rt = new RabbitTemplate(connectionFactory);
        rt.setMessageConverter(jackson2JsonMessageConverter);
        return rt;
    }

    @Bean
    public MessageConverter contentTypeDelegatingMessageConverter(Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        // delegating: mặc định dùng Jackson (JSON)
        ContentTypeDelegatingMessageConverter delegating =
                new ContentTypeDelegatingMessageConverter(jackson2JsonMessageConverter);

        // Concrete converter cho Java-serialized objects
        SimpleMessageConverter javaSerializedConverter = new SimpleMessageConverter();

        // Important: set classloader so deserialization dùng classloader của app
        javaSerializedConverter.setBeanClassLoader(this.getClass().getClassLoader());

        // Allowed patterns - điều chỉnh cho phù hợp (nên ưu tiên package cụ thể của bạn)
        // Ví dụ: chỉ cho phép HashMap và các lớp trong project com.example.*
        List<String> allowed = Arrays.asList(
                "java.util.HashMap",
                "java.util.*",
                "com.example.*"
        );
        // addAllowedListPatterns(...) có sẵn trên AllowedListDeserializingMessageConverter (SimpleMessageConverter kế thừa)
        javaSerializedConverter.addAllowedListPatterns(allowed.toArray(new String[0]));

        // Register delegates for common content-type forms (some producers include charset)
        delegating.addDelegate("application/x-java-serialized-object", javaSerializedConverter);
        delegating.addDelegate("application/x-java-serialized-object; charset=UTF-8", javaSerializedConverter);
        delegating.addDelegate("application/octet-stream", javaSerializedConverter);

        // (optional) If you prefer to treat unknown/no-content-type as JSON leave default as Jackson.
        // delegating constructed above already defaults to jackson2JsonMessageConverter.

        return delegating;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter contentTypeDelegatingMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(contentTypeDelegatingMessageConverter);
        return factory;
    }
}
