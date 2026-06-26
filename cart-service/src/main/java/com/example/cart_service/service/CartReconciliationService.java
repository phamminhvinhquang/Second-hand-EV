package com.example.cart_service.service;

import com.example.cart_service.client.ProductServiceClient;
import com.example.cart_service.repository.CartRepository;
import com.example.cart_service.model.Cart;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.List;

// Service chạy tác vụ reconcile giỏ hàng: kiểm tra product còn tồn tại bằng product-service
// - Có thể chạy manual (runReconciliation), cho từng user (reconcileForUser) hoặc theo lịch (reconcileOrphanedCarts).
@Service
public class CartReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(CartReconciliationService.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductServiceClient productClient;

    // Manual: quét tất cả productId có trong cart, gọi product-service để verify.
    // Nếu product-service trả 404 => xóa tất cả cart rows liên quan, trả tổng số bản ghi đã xóa.
    public int runReconciliation() {
        log.info("[Manual] Bắt đầu tác vụ dọn dẹp giỏ hàng (manual trigger)...");

        Set<Long> productIdsInCart = cartRepository.findAllDistinctProductIds();
        if (productIdsInCart == null || productIdsInCart.isEmpty()) {
            log.info("[Manual] Không có sản phẩm nào trong giỏ hàng. Kết thúc.");
            return 0;
        }

        log.info("[Manual] Tìm thấy {} productId duy nhất. Đang kiểm tra...", productIdsInCart.size());
        int cleanedCount = 0;

        for (Long productId : productIdsInCart) {
            try {
                // Gọi Feign Client để kiểm tra tồn tại
                productClient.getProductDetail(productId);
                // nếu trả OK => sản phẩm tồn tại, không làm gì
            } catch (Exception e) {
                // Nếu là lỗi 404 từ Feign => sản phẩm không tồn tại nữa
                if (e instanceof FeignException && ((FeignException) e).status() == 404) {
                    log.warn("[Manual] Phát hiện productId mồ côi: {}. Sẽ tự động xóa.", productId);
                    cartRepository.deleteByProductId(productId);
                    cleanedCount++;
                } else {
                    // Lỗi khác (listing-service down, network, v.v.) -> log và bỏ qua
                    log.error("[Manual] Lỗi khi kiểm tra productId {}: {}", productId, e.getMessage());
                }
            }
        }

        log.info("[Manual] Tác vụ dọn dẹp hoàn tất. Đã xóa {} sản phẩm mồ côi.", cleanedCount);
        return cleanedCount;
    }

    // Reconcile chỉ cho 1 user: quét cart của user, verify từng item, xóa những item product không tồn tại.
    // Trả về số cart rows đã xóa cho user đó.
    public int reconcileForUser(Long userId) {
        if (userId == null) return 0;
        log.info("Reconciling carts for userId={}", userId);
        List<Cart> carts = cartRepository.findByUserIdOrderByIdDesc(userId);
        int cleaned = 0;
        for (Cart c : carts) {
            Long pid = c.getProductId();
            if (pid == null) {
                try {
                    cartRepository.deleteById(c.getId());
                    cleaned++;
                } catch (Exception e) {
                    log.warn("Failed to delete cart id {} with null productId: {}", c.getId(), e.getMessage());
                }
                continue;
            }
            try {
                productClient.getProductDetail(pid);
            } catch (Exception e) {
                if (e instanceof FeignException && ((FeignException) e).status() == 404) {
                    try {
                        cartRepository.deleteById(c.getId());
                        cleaned++;
                        log.info("Deleted cart id {} for missing product {}", c.getId(), pid);
                    } catch (Exception de) {
                        log.warn("Failed to delete orphan cart id {}: {}", c.getId(), de.getMessage());
                    }
                } else {
                    log.debug("Non-fatal error while verifying product {} for user {}: {}", pid, userId, e.getMessage());
                }
            }
        }
        log.info("Reconcile for user {} done. Deleted {}", userId, cleaned);
        return cleaned;
    }

    // Scheduled task chạy theo cron (hằng ngày lúc 03:00) để tự động dọn các product mồ côi.
    @Scheduled(cron = "0 0 1 * * *")
    public void reconcileOrphanedCarts() {
        log.info("[Scheduler] Bắt đầu tác vụ dọn dẹp giỏ hàng...");
        int cleaned = runReconciliation();
        log.info("[Scheduler] Tác vụ dọn dẹp theo lịch hoàn tất. Đã xóa {} sản phẩm mồ côi.", cleaned);
    }
}
