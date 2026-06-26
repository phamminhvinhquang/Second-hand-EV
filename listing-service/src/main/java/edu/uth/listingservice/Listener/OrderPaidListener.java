
package edu.uth.listingservice.Listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.uth.listingservice.DTO.OrderCompletedEventDTO; 

import edu.uth.listingservice.Service.ProductListingService;

@Component
public class OrderPaidListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPaidListener.class);

    @Autowired
    private ProductListingService listingService; // Service để "mark as sold"

    /**
     * Lắng nghe sự kiện từ Transaction-Service
     * để đánh dấu sản phẩm là "Đã bán".
     */
    @RabbitListener(queues = "${app.rabbitmq.order-events.queue}")
    public void handleOrderPaid(OrderCompletedEventDTO dto) {
        
        if (dto == null || dto.getProductId() == null) {
            log.warn("❌ [MQ] Nhận được sự kiện order.paid không hợp lệ (thiếu productId).");
            return;
        }

        log.info("✅ [MQ] Nhận sự kiện OrderPaid cho productId: {}", dto.getProductId());
        try {
            // Gọi service để đánh dấu đã bán
            // (Bạn đã có hàm này trong ProductListingController)
            listingService.markAsSold(dto.getProductId());
            log.info("✅ Đã đánh dấu 'Đã bán' cho productId: {}", dto.getProductId());
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý sự kiện order.paid cho productId: {}: {}", dto.getProductId(), e.getMessage());
            // (Cần xử lý retry hoặc dead-letter-queue sau)
        }
    }
}