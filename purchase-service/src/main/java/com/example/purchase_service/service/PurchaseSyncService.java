package com.example.purchase_service.service;

import com.example.purchase_service.dto.PurchaseDTO;
import com.example.purchase_service.model.Purchase;
import com.example.purchase_service.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Trước đây admin-service fetch purchases từ purchase-service và persist sang DB riêng.
 * Giờ đã gộp — syncAllPurchases đơn giản trả về tất cả purchases hiện có.
 */
@Service
@RequiredArgsConstructor
public class PurchaseSyncService {

    private final PurchaseRepository purchaseRepository;

    @Transactional
    public List<Purchase> syncAllPurchases() {
        // đơn giản: trả về tất cả purchases hiện có
        return purchaseRepository.findAll();
    }

    // optional: scheduled task (không bắt buộc)
    @Scheduled(fixedDelayString = "${sync.purchases.delay:300000}")
    public void scheduledSync() {
        try {
            syncAllPurchases();
        } catch (Exception e) {
            System.err.println("Scheduled sync failed: " + e.getMessage());
        }
    }
}
