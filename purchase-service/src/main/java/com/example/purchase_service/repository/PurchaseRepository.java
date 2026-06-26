package com.example.purchase_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.purchase_service.model.Purchase;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Purchase> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    Optional<Purchase> findByTransactionId(String transactionId);
    boolean existsByTransactionId(String transactionId);

    //Tìm đơn hàng theo Product ID
    Optional<Purchase> findByProductId(Long productId);
}
