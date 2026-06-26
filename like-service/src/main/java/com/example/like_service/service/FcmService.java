package com.example.like_service.service;

import com.example.like_service.repository.FcmTokenRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);
    private final FcmTokenRepository tokenRepo;

    public FcmService(FcmTokenRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    /**
     * Gửi data-only messages cho list token (batch). Nếu token invalid -> xóa khỏi DB.
     * data map nên chứa các trường: title, body, link, productId, newPrice, oldPrice, ...
     */
    public void sendNotificationsToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) return;

        // ensure title/body also available in data for SW to read
        Map<String, String> payloadData = new HashMap<>();
        if (data != null) payloadData.putAll(data);
        if (title != null) payloadData.putIfAbsent("title", title);
        if (body != null) payloadData.putIfAbsent("body", body);

        List<Message> messages = tokens.stream().map(token -> {
            // Build message with data only (no top-level Notification)
            Message.Builder b = Message.builder()
                    .setToken(token)
                    .putAllData(payloadData)
                    .setWebpushConfig(WebpushConfig.builder()
                            .putAllData(payloadData)
                            .build());

            return b.build();
        }).collect(Collectors.toList());

        try {
            BatchResponse resp = FirebaseMessaging.getInstance().sendAll(messages);
            log.info("FCM sendAll: successCount={} failureCount={}", resp.getSuccessCount(), resp.getFailureCount());

            List<String> toRemove = new ArrayList<>();
            for (int i = 0; i < resp.getResponses().size(); i++) {
                SendResponse sr = resp.getResponses().get(i);
                if (!sr.isSuccessful()) {
                    String tok = tokens.get(i);
                    String err = sr.getException() != null ? sr.getException().getMessage() : null;
                    boolean remove = false;

                    if (sr.getException() instanceof com.google.firebase.messaging.FirebaseMessagingException) {
                        try {
                            com.google.firebase.messaging.FirebaseMessagingException fme =
                                    (com.google.firebase.messaging.FirebaseMessagingException) sr.getException();

                            // getErrorCode() may return an enum or object in this SDK version.
                            // Convert to String safely:
                            String codeStr = null;
                            try {
                                Object codeObj = fme.getErrorCode();
                                if (codeObj != null) codeStr = codeObj.toString();
                            } catch (Throwable ignored) {
                            }

                            if (codeStr != null) {
                                String c = codeStr.toLowerCase(Locale.ROOT);
                                if (c.contains("registration_token_not_registered")
                                        || c.contains("not_registered")
                                        || c.contains("invalid_registration_token")) {
                                    remove = true;
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }

                    if (!remove && err != null) {
                        String m = err.toLowerCase(Locale.ROOT);
                        if (m.contains("registration-token-not-registered")
                                || m.contains("invalid-registration-token")
                                || m.contains("not-registered")) {
                            remove = true;
                        }
                    }

                    if (remove) {
                        toRemove.add(tok);
                        log.info("Mark token for removal: {} cause={}", tok, err);
                    } else {
                        log.warn("FCM send failed for token={}, error={}", tok, err);
                    }
                }
            }

            if (!toRemove.isEmpty()) {
                toRemove.forEach(tokenRepo::deleteByToken);
                log.info("Removed {} invalid tokens from DB", toRemove.size());
            }

        } catch (Exception ex) {
            log.error("Error sending FCM batch", ex);
        }
    }
}
