package com.example.purchase_service.controller;

import com.example.purchase_service.mq.ComplaintMqListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/stream")
public class ComplaintSseController {

    @GetMapping(path = "/admin", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter adminStream(@RequestParam(value = "adminUserId", required = false) String adminUserId) {
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
        ComplaintMqListener.registerAdminEmitter(adminUserId == null ? "" : adminUserId, emitter);
        emitter.onCompletion(() -> {});
        emitter.onTimeout(() -> {});
        emitter.onError((e) -> {});
        try { emitter.send(SseEmitter.event().name("connected").data("ok")); } catch (IOException ignored) {}
        return emitter;
    }

    @GetMapping(path = "/user", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter userStream(@RequestParam(value = "userId", required = false) String userId) {
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
        ComplaintMqListener.registerUserEmitter(userId == null ? "" : userId, emitter);
        emitter.onCompletion(() -> {});
        emitter.onTimeout(() -> {});
        emitter.onError((e) -> {});
        try { emitter.send(SseEmitter.event().name("connected").data("ok")); } catch (IOException ignored) {}
        return emitter;
    }
}
