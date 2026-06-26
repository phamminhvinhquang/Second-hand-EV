package com.example.purchase_service.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens to complaint.* topic keys and forwards to connected SSE clients.
 * - For admin messages we broadcast to all connected admin SSE clients (so any admin UI gets the notification).
 * - For user messages we route by userId key.
 */
@Component
@Slf4j
public class ComplaintMqListener {

    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> adminEmitters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    public static void registerAdminEmitter(String adminId, SseEmitter emitter) {
        String key = adminId == null ? "" : adminId;
        adminEmitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("Registered admin SSE emitter for adminId={}. total admin keys={}", key, adminEmitters.size());
        emitter.onCompletion(() -> adminEmitters.getOrDefault(key, new CopyOnWriteArrayList<>()).remove(emitter));
        emitter.onTimeout(() -> adminEmitters.getOrDefault(key, new CopyOnWriteArrayList<>()).remove(emitter));
    }

    public static void registerUserEmitter(String userId, SseEmitter emitter) {
        String key = userId == null ? "" : userId;
        userEmitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("Registered user SSE emitter for userId={}. total user keys={}", key, userEmitters.size());
        emitter.onCompletion(() -> userEmitters.getOrDefault(key, new CopyOnWriteArrayList<>()).remove(emitter));
        emitter.onTimeout(() -> userEmitters.getOrDefault(key, new CopyOnWriteArrayList<>()).remove(emitter));
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "admin.complaint.queue", durable = "true"),
            exchange = @Exchange(value = "${mq.exchange:ev.exchange}", type = ExchangeTypes.TOPIC, durable = "true"),
            key = "complaint.to.admin"
    ))
    public void onComplaintToAdmin(Map<String, Object> payload) {
        try {
            log.info("[MQ] complaint.to.admin -> {}", payload);
            // Broadcast to ALL admin emitters (so any logged-in admin receives)
            for (CopyOnWriteArrayList<SseEmitter> list : adminEmitters.values()) {
                if (list == null) continue;
                for (SseEmitter e : list) {
                    try {
                        e.send(SseEmitter.event().name("message").data(payload));
                    } catch (Exception ex) {
                        list.remove(e);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error in onComplaintToAdmin", ex);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "user.complaint.queue", durable = "true"),
            exchange = @Exchange(value = "${mq.exchange:ev.exchange}", type = ExchangeTypes.TOPIC, durable = "true"),
            key = "complaint.to.user"
    ))
    public void onComplaintToUser(Map<String, Object> payload) {
        try {
            log.info("[MQ] complaint.to.user -> {}", payload);
            String userId = payload.get("userId") != null ? String.valueOf(payload.get("userId")) : "";
            CopyOnWriteArrayList<SseEmitter> list = userEmitters.get(userId);
            if (list != null) {
                for (SseEmitter e : list) {
                    try {
                        e.send(SseEmitter.event().name("message").data(payload));
                    } catch (Exception ex) {
                        list.remove(e);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error in onComplaintToUser", ex);
        }
    }
}
