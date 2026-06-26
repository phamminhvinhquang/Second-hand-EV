// File: com/example/cart_service/listener/CartRequestListener.java
package com.example.cart_service.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.example.cart_service.model.Cart;
import com.example.cart_service.repository.CartRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartRequestListener {

    private final RabbitTemplate rabbitTemplate;
    private final CartRepository cartRepository;

    // 📨 Lắng nghe yêu cầu lấy giỏ hàng từ transaction-service
    @RabbitListener(queues = "cart.fetch.request")
    public void handleCartFetchRequest(Map<String, Object> data) {
        try {
            String transactionId = String.valueOf(data.get("transactionId"));
            Long userId = data.get("userId") != null
                    ? Long.parseLong(String.valueOf(data.get("userId")))
                    : null;

            // 🧾 Ép kiểu an toàn cho cartIds
            List<Long> cartIds = new ArrayList<>();
            Object rawCartIds = data.get("cartIds");
            if (rawCartIds instanceof List<?>) {
                for (Object o : (List<?>) rawCartIds) {
                    if (o != null) {
                        try {
                            cartIds.add(Long.parseLong(o.toString()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // 🔍 Lấy dữ liệu giỏ hàng
            List<Cart> carts;
            if (!cartIds.isEmpty()) {
                carts = cartRepository.findAllById(cartIds);
            } else if (userId != null) {
                carts = cartRepository.findByUserIdOrderByIdDesc(userId);
            } else {
                carts = List.of();
            }

            // 🧩 Đóng gói phản hồi trả về cho transaction-service
            List<Map<String, Object>> items = new ArrayList<>();
            for (Cart c : carts) {
                Map<String, Object> item = new HashMap<>();
                item.put("cartId", c.getId());
                item.put("productName", c.getProductname());
                item.put("price", c.getPrice());
                item.put("sellerId", c.getSellerId());
                
                // ⭐⭐⭐ [THÊM DÒNG NÀY] ⭐⭐⭐
                item.put("productId", c.getProductId()); 
                // ⭐⭐⭐ [HẾT SỬA] ⭐⭐⭐

                items.add(item);
            }

            Map<String, Object> response = Map.of(
                    "transactionId", transactionId,
                    "items", items
            );

            // 📤 Gửi lại phản hồi qua MQ
            rabbitTemplate.convertAndSend("ev.exchange", "cart.fetch.response", response);
            log.info("📤 [CartService] Gửi phản hồi giỏ hàng ({} items) cho transaction {}", items.size(), transactionId);

        } catch (Exception e) {
            log.error("❌ [CartService] Lỗi khi xử lý cart.fetch.request: {}", e.getMessage(), e);
        }
    }
}