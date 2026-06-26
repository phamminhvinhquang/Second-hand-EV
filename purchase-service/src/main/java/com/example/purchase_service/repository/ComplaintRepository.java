package com.example.purchase_service.repository;

import com.example.purchase_service.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByPurchaseIdOrderByCreatedAtDesc(Long purchaseId);
    long countByPurchaseId(Long purchaseId);
}
