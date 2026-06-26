package com.example.cart_service.listener;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.cart_service.repository.CartRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidListener {

    private final CartRepository cartRepository;

    // 📨 Nhận sự kiện khi đơn hàng đã thanh toán thành công
    @RabbitListener(queues = "cart.order.paid")
    public void handleOrderPaid(Map<String, Object> data) {
        log.info("🧾 [CartService] Nhận event order.paid: {}", data);
        try {
            Long userId = getLongValue(data.get("userId"));
            Object cartIdsObj = data.get("cartIds");

            // ✅ CASE 1: Có danh sách cartIds (Logic mới - Chỉ xóa món đã mua)
            if (cartIdsObj instanceof List && !((List<?>) cartIdsObj).isEmpty()) {
                List<?> rawList = (List<?>) cartIdsObj;
                
                // Convert list an toàn sang List<Long> (tránh lỗi ClassCastException Integer -> Long)
                List<Long> targetIds = rawList.stream()
                        .map(this::getLongValue)
                        .filter(id -> id != null)
                        .collect(Collectors.toList());

                if (!targetIds.isEmpty()) {
                    // JpaRepository có sẵn deleteAllById, chỉ xóa đúng các ID này
                    cartRepository.deleteAllById(targetIds); 
                    log.info("✅ Đã xóa {} sản phẩm đã thanh toán theo ID: {}", targetIds.size(), targetIds);
                }
            } 
            // ⚠️ CASE 2: Fallback (Logic cũ) - Chỉ chạy nếu payment-service không gửi cartIds
            else if (userId != null) {
                cartRepository.deleteByUserId(userId);
                log.warn("⚠️ Event không có cartIds, đã xóa TOÀN BỘ giỏ hàng của user #{}", userId);
            } 
            else {
                log.warn("⚠️ Bỏ qua sự kiện order.paid vì thiếu cả cartIds và userId");
            }

        } catch (Exception e) {
            log.error("❌ [CartService] Lỗi khi xử lý order.paid: {}", e.getMessage(), e);
        }
    }

    // Hàm phụ để convert Number/String sang Long an toàn
    private Long getLongValue(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}