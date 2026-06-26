package com.example.purchase_service.service;

import com.example.purchase_service.model.Purchase;
import com.example.purchase_service.dto.CreatePurchaseRequest;

import java.util.List;
import java.util.Map;

public interface PurchaseService {

    Purchase createNewPurchase(Purchase purchase);

    
    Purchase createPurchase(CreatePurchaseRequest req);

    List<Purchase> getPurchasesForUser(Long userId);

    List<Purchase> getPurchasesForUserByStatus(Long userId, String status);
    
    Purchase getPurchaseById(Long id);
    
    Purchase createPurchaseFromTransaction(String transactionId);

    boolean existsByTransactionId(String transactionId);

    // new: trả về tất cả purchases (dùng oleh admin sync)
    List<Purchase> getAllPurchases();

    /**
     * 🛑 HÀM MỚI: Tạo Purchase từ payload sự kiện MQ (chứa đầy đủ thông tin)
     * @param eventPayload Payload từ event 'order.paid'
     * @return Purchase đã lưu
     */
    Purchase createPurchaseFromEvent(Map<String, Object> eventPayload);
}
