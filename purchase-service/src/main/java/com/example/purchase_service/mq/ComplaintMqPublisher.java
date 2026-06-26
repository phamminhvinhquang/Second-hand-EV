package com.example.purchase_service.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComplaintMqPublisher {

    private final RabbitTemplate rabbitTemplate;

    // injected exchange name (default ev.exchange)
    @Value("${mq.exchange:ev.exchange}")
    private String mqExchange;

    public void publishToAdmin(Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(mqExchange, "complaint.to.admin", payload);
            log.info("Published complaint -> admin on exchange={} routingKey=complaint.to.admin payload={}", mqExchange, payload);
        } catch (Exception ex) {
            log.error("Failed to publish to admin exchange: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    public void publishToUser(Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(mqExchange, "complaint.to.user", payload);
            log.info("Published complaint -> user on exchange={} routingKey=complaint.to.user payload={}", mqExchange, payload);
        } catch (Exception ex) {
            log.error("Failed to publish to user exchange: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
