package com.example.purchase_service.controller;

import com.example.purchase_service.dto.PurchaseDTO;
import com.example.purchase_service.model.Purchase;
import com.example.purchase_service.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    public ResponseEntity<Purchase> createPurchase(@RequestBody Purchase purchase) {
        // Bạn sẽ cần implement logic này trong PurchaseService
        // Ví dụ: gán createdAt, status mặc định, rồi lưu vào DB
        Purchase newPurchase = purchaseService.createNewPurchase(purchase); 
        return ResponseEntity.status(201).body(newPurchase); // 201 = Created
    }
    
    // GET /api/purchases?userId=...
    @GetMapping
    public ResponseEntity<List<Purchase>> getPurchases(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(purchaseService.getPurchasesForUser(userId));
    }

    // GET /api/purchases/all  -> trả về tất cả purchases (dùng bởi admin-sync)
    @GetMapping("/all")
    public ResponseEntity<List<PurchaseDTO>> getAllPurchases() {
        List<PurchaseDTO> out = purchaseService.getAllPurchases().stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    // Create purchase from transaction (used by frontend: /purchase.html after tx)
    @GetMapping("/from-transaction/{tx}")
    public ResponseEntity<Purchase> createFromTransaction(@PathVariable("tx") String tx) {
        Purchase p = purchaseService.createPurchaseFromTransaction(tx);
        return ResponseEntity.ok(p);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Purchase> getById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(id));
    }

    private PurchaseDTO toDto(Purchase p) {
        PurchaseDTO d = new PurchaseDTO();
        d.setId(p.getId());
        d.setTransactionId(p.getTransactionId());
        d.setUserId(p.getUserId());
        d.setSellerId(p.getSellerId());
        d.setProductId(p.getProductId());
        d.setProductName(p.getProductName());
        d.setProductImage(p.getProductImage());
        d.setPrice(p.getPrice());
        d.setStatus(p.getStatus());
        d.setFullName(p.getFullName());
        d.setPhone(p.getPhone());
        d.setEmail(p.getEmail());
        d.setAddress(p.getAddress());
        d.setCreatedAt(p.getCreatedAt());
        // imageUrls not available here; kept null
        return d;
    }
}
