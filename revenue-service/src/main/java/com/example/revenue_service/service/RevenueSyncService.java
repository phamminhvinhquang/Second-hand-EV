
package com.example.revenue_service.service;

import com.example.revenue_service.dto.PurchaseDTO;
// Đã xóa import RevenueSyncPublisher
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueSyncService {

    private final RestTemplate restTemplate;
    // ✅ Thay đổi: Inject trực tiếp RevenueService để xử lý
    private final RevenueService revenueService;

    @Value("${purchase.service.url:http://localhost:8096}")
    private String purchaseServiceUrl;
    
    public int syncAllPurchases() {
        String url = purchaseServiceUrl.endsWith("/") ? 
                     purchaseServiceUrl + "api/purchases/all" : 
                     purchaseServiceUrl + "/api/purchases/all";

        log.info("🔄 Starting sync: Fetching from {}", url);
        
        try {
            ResponseEntity<List<PurchaseDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PurchaseDTO>>() {}
            );

            List<PurchaseDTO> purchases = response.getBody();
            if (purchases == null || purchases.isEmpty()) {
                log.info("No purchases found.");
                return 0;
            }
            
            int count = 0;
            for (PurchaseDTO p : purchases) {
                String status = p.getStatus() != null ? p.getStatus().toLowerCase() : "";

                // Chấp nhận cả "completed" VÀ "returned"
                if ("completed".equals(status) || "returned".equals(status)) {
                    // ✅ THAY ĐỔI: Gọi hàm xử lý trực tiếp (Synchronous) thay vì đẩy vào RabbitMQ
                    revenueService.importFromPurchase(p);
                    count++;
                }
            }
            
            log.info("✅ Sync Completed: Processed {} items directly.", count);
            return count;

        } catch (Exception e) {
            log.error("Failed to pull purchases: {}", e.getMessage());
            return 0;
        }
    }

    public void scheduledSync() {
        log.info("Java Scheduler is disabled. Using Apache NiFi for data sync.");
    }
}