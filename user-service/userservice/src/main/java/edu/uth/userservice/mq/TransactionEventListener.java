// File: edu/uth/userservice/mq/TransactionEventListener.java
package edu.uth.userservice.mq;

import edu.uth.userservice.model.TransactionHistory;
import edu.uth.userservice.repository.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor 
public class TransactionEventListener {

    private final TransactionHistoryRepository historyRepo;

    /**
     * ⭐️ Lắng nghe sự kiện MUA HÀNG từ transaction-service
     */
    //@RabbitListener(queues = "user.history.order_paid")
    public void handleOrderPaid(Map<String, Object> event) {
        log.info("Nhận sự kiện 'order.paid': {}", event);
        try {
            TransactionHistory history = new TransactionHistory();
            
            // ⭐️⭐️⭐️ [SỬA LỖI TẠI ĐÂY] ⭐️⭐️⭐️
            // Đọc ID người mua từ key "userId" (thay vì "buyerId")
            history.setUserId(((Number) event.get("userId")).intValue()); 
            // ⭐️⭐️⭐️ [KẾT THÚC SỬA LỖI] ⭐️⭐️⭐️

            history.setSellerId(((Number) event.get("sellerId")).longValue());
            history.setTransactionId((String) event.get("transactionId"));
            history.setAmount(new BigDecimal(event.get("price").toString()));
            history.setMethod((String) event.get("method"));
            history.setStatus("SUCCESS");
            history.setType("order");
            history.setCreatedAt(LocalDateTime.now());

            historyRepo.save(history);
            log.info("✅ Đã lưu lịch sử 'order' cho user: {}", history.getUserId());

        } catch (Exception e) {
            log.error("❌ Lỗi xử lý sự kiện 'order.paid': {}", e.getMessage(), e);
        }
    }

    /**
     * ⭐️ Lắng nghe sự kiện NẠP TIỀN từ transaction-service
     * (Hàm này đã chính xác, giữ nguyên)
     */
   // @RabbitListener(queues = "user.history.deposit_success")
    public void handleDepositSuccess(Map<String, Object> event) {
        log.info("Nhận sự kiện 'wallet.deposit.success': {}", event);
        try {
            TransactionHistory history = new TransactionHistory();

            // Lấy ID người nạp từ key "userId"
            history.setUserId(((Number) event.get("userId")).intValue());
            history.setSellerId(null); 
            history.setTransactionId((String) event.get("transactionId"));
            history.setAmount(new BigDecimal(event.get("amount").toString()));
            history.setMethod((String) event.get("method"));
            history.setStatus("SUCCESS");
            history.setType("deposit");
            history.setCreatedAt(LocalDateTime.now());
            // 👇👇👇 THÊM ĐOẠN NÀY ĐỂ LẤY THÔNG TIN XE 👇👇👇
        if (event.containsKey("productName")) {
            history.setProductName((String) event.get("productName"));
        } else {
            history.setProductName("Sản phẩm xe điện"); // Giá trị mặc định
        }

        if (event.containsKey("productImage")) {
            history.setProductImg((String) event.get("productImage"));
        }
        // 👆👆👆 KẾT THÚC THÊM 👆👆👆

            historyRepo.save(history);
            log.info("✅ Đã lưu lịch sử 'deposit' cho user: {}", history.getUserId());
            
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý sự kiện 'deposit.success': {}", e.getMessage(), e);
        }
    }
}