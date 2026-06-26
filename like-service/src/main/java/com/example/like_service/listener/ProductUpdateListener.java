package com.example.like_service.listener;

import com.example.like_service.model.FcmToken;
import com.example.like_service.model.Like;
import com.example.like_service.model.Notification;
import com.example.like_service.repository.FcmTokenRepository;
import com.example.like_service.repository.LikeRepository;
import com.example.like_service.repository.NotificationRepository;
import com.example.like_service.service.FcmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Listener chung cho "product update" events.
 * Nhận payload dạng Map (Jackson -> Map) để xử lý nhiều loại event.
 */
@Component
public class ProductUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(ProductUpdateListener.class);
    private final LikeRepository likeRepository;
    private final FcmTokenRepository tokenRepository;
    private final NotificationRepository notificationRepository;
    private final FcmService fcmService;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final int BATCH_SIZE = 500;

    public ProductUpdateListener(LikeRepository likeRepository,
                                 FcmTokenRepository tokenRepository,
                                 NotificationRepository notificationRepository,
                                 FcmService fcmService) {
        this.likeRepository = likeRepository;
        this.tokenRepository = tokenRepository;
        this.notificationRepository = notificationRepository;
        this.fcmService = fcmService;
    }

    @RabbitListener(queues = "${rabbitmq.queue:product.update.queue}")
    @Transactional
    public void onProductUpdate(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            log.debug("Ignoring empty product update payload");
            return;
        }

        Long productId = extractLong(payload, "productId", "product_id", "id");
        String eventType = asString(payload.get("eventType"), payload.get("event_type"));
        Long newPrice = extractLong(payload, "newPrice", "new_price", "price"); // try different keys
        Long oldPrice = extractLong(payload, "oldPrice", "old_price");

        // Try nested product object if top-level id not present
        if (productId == null) {
            Object nested = payload.get("product");
            if (nested instanceof Map) {
                productId = extractLong((Map<String, Object>) nested, "productId", "product_id", "id");
            }
        }

        if (productId == null) {
            log.warn("Product update event missing productId: {}", payload);
            return;
        }

        // Make a final copy so it can be used inside lambdas/streams / inner classes
        final Long finalProductId = productId;

        // Build title/body depending on event
        String title;
        String body;

        if ("LISTING_DELETED".equalsIgnoreCase(eventType) || "DELETED".equalsIgnoreCase(eventType)) {
            title = "Tin đăng đã bị xóa";
            body = "Tin đăng bạn đã lưu vừa bị xóa.";
        } else {
            title = "Tin đăng bạn đã lưu có thay đổi";
            StringBuilder sb = new StringBuilder();
            if (oldPrice != null || newPrice != null) {
                sb.append("Giá: ");
                if (oldPrice != null && newPrice != null) {
                    sb.append(formatMoney(oldPrice)).append(" → ").append(formatMoney(newPrice));
                } else if (newPrice != null) {
                    sb.append("mới ").append(formatMoney(newPrice));
                } else {
                    sb.append("đã thay đổi");
                }
            }
            String oldCond = asString(payload.get("oldConditionName"), payload.get("old_condition_name"), payload.get("old_condition"));
            String newCond = asString(payload.get("newConditionName"), payload.get("new_condition_name"), payload.get("new_condition"));
            if (sb.length() > 0 && (oldCond != null || newCond != null)) sb.append("; ");
            if (oldCond != null || newCond != null) {
                sb.append("Tình trạng: ");
                if (oldCond != null && newCond != null) sb.append(oldCond).append(" → ").append(newCond);
                else if (newCond != null) sb.append("mới ").append(newCond);
                else sb.append("đã thay đổi");
            }
            if (sb.length() == 0) sb.append("Một thay đổi nhỏ đã xảy ra trên tin đăng bạn lưu.");
            body = sb.toString();
        }

        // Prepare payload data for FCM (data-only) — use finalProductId
        Map<String, String> data = new HashMap<>();
        data.put("productId", String.valueOf(finalProductId));
        if (newPrice != null) data.put("newPrice", String.valueOf(newPrice));
        if (oldPrice != null) data.put("oldPrice", String.valueOf(oldPrice));
        if (eventType != null) data.put("eventType", eventType);
        data.put("link", "/product_detail.html?id=" + finalProductId);
        data.put("title", title);
        data.put("body", body);

        // Load likes for this product
        List<Like> likes = likeRepository.findByProductId(finalProductId);
        if (likes == null || likes.isEmpty()) {
            log.debug("No likes for productId={}", finalProductId);
            return;
        }

        // Distinct user ids
        Set<Long> userIds = likes.stream()
                .map(Like::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            log.debug("No userIds to notify for productId={}", finalProductId);
            return;
        }

        // --- LƯU Notification vào DB cho mỗi user trước khi gửi FCM ---
        try {
            // attempt to resolve product name: try from payload then from likes list
            String productName = null;
            // try payload first (may be present)
            Object pn = payload.get("productName");
            if (pn == null) pn = payload.get("product_name");
            if (pn != null) productName = String.valueOf(pn);

            // fallback: try find from one of the Like records (they contain productname)
            if (productName == null || productName.trim().isEmpty()) {
                productName = likes.stream()
                        .map(Like::getProductname)
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .filter(s -> !s.trim().isEmpty())
                        .findFirst()
                        .orElse(null);
            }

            for (Long uid : userIds) {
                try {
                    Optional<Notification> existingOpt =
                            notificationRepository.findTopByUserIdAndProductIdOrderByCreatedAtDesc(uid, finalProductId);

                    if (existingOpt.isPresent()) {
                        Notification existing = existingOpt.get();
                        // update content but keep same id (overwrite)
                        existing.setTitle(title);
                        existing.setBody(body);
                        existing.setLink("/product_detail.html?id=" + finalProductId);
                        existing.setSeen(false);
                        if (productName != null) existing.setProductName(productName);
                        notificationRepository.save(existing);
                    } else {
                        Notification n = Notification.builder()
                                .userId(uid)
                                .title(title)
                                .body(body)
                                .productId(finalProductId)
                                .link("/product_detail.html?id=" + finalProductId)
                                .seen(false)
                                .productName(productName)
                                .build();
                        notificationRepository.save(n);
                    }
                } catch (Exception singleEx) {
                    log.warn("Failed to save/update notification for userId={} productId={}: {}", uid, finalProductId, singleEx.getMessage());
                }
            }
            log.debug("Saved/updated notifications (with productName) for productId={}", finalProductId);
        } catch (Exception e) {
            log.warn("Failed to save notifications: {}", e.getMessage());
        }
        // ----------------------------------------------------------------

        // Collect tokens for those users
        LinkedHashSet<String> tokenSet = new LinkedHashSet<>();
        for (Long uid : userIds) {
            try {
                List<FcmToken> toks = tokenRepository.findByUserId(uid);
                if (toks != null) {
                    toks.stream().map(FcmToken::getToken).filter(Objects::nonNull).forEach(tokenSet::add);
                }
            } catch (Exception e) {
                log.warn("Failed to load tokens for userId={}: {}", uid, e.getMessage());
            }
        }
        if (tokenSet.isEmpty()) {
            log.debug("No FCM tokens found for productId={}", finalProductId);
            return;
        }

        // title/body already prepared above
        List<String> tokens = new ArrayList<>(tokenSet);
        log.info("Sending notifications to {} tokens ({} users) for productId={}", tokens.size(), userIds.size(), finalProductId);

        if (tokens.size() <= BATCH_SIZE) {
            fcmService.sendNotificationsToTokens(tokens, title, body, data);
        } else {
            int batches = (tokens.size() + BATCH_SIZE - 1) / BATCH_SIZE;
            for (int i = 0; i < batches; i++) {
                int from = i * BATCH_SIZE;
                int to = Math.min(from + BATCH_SIZE, tokens.size());
                List<String> sub = tokens.subList(from, to);
                fcmService.sendNotificationsToTokens(sub, title, body, data);
            }
        }
    }


    // helpers
    private Long extractLong(Map<String, Object> m, String... keys) {
        if (m == null) return null;
        for (String k : keys) {
            Object v = m.get(k);
            if (v == null) continue;
            if (v instanceof Number) return ((Number) v).longValue();
            try {
                return Long.parseLong(v.toString());
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String asString(Object... candidates) {
        for (Object c : candidates) {
            if (c == null) continue;
            String s = c.toString();
            if (!s.trim().isEmpty()) return s;
        }
        return null;
    }

    private String formatMoney(Long v) {
        if (v == null) return "";
        // simple formatting without locale libs
        String s = String.valueOf(v);
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        int cnt = 0;
        for (int i = len - 1; i >= 0; i--) {
            sb.append(s.charAt(i));
            cnt++;
            if (cnt == 3 && i != 0) {
                sb.append('.');
                cnt = 0;
            }
        }
        return new StringBuilder(sb.toString()).reverse().toString();
    }
}