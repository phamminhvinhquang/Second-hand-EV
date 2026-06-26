package com.example.purchase_service.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.purchase_service.model.Complaint;
import com.example.purchase_service.model.ComplaintMessage;
import com.example.purchase_service.model.Purchase;
import com.example.purchase_service.mq.ComplaintMqPublisher;
import com.example.purchase_service.repository.ComplaintMessageRepository;
import com.example.purchase_service.repository.ComplaintRepository;
import com.example.purchase_service.repository.PurchaseRepository;
import com.example.purchase_service.service.PurchaseSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin-trans")
@RequiredArgsConstructor
@Slf4j
public class AdminTransactionController {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseSyncService purchaseSyncService;
    private final ComplaintRepository complaintRepository;
    private final ComplaintMessageRepository complaintMessageRepository;
    private final ComplaintMqPublisher mqPublisher;

    @Value("${user.service.url:http://localhost:8084}")
    private String userServiceUrl;

    /**
     * GET /transactions/all
     */
    @GetMapping("/transactions/all")
    public ResponseEntity<List<Purchase>> getAllTransactions() {
        try {
            return ResponseEntity.ok(
                    purchaseRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        } catch (Exception e) {
            return ResponseEntity.ok(purchaseRepository.findAll());
        }
    }

    /**
     * DELETE transaction
     */
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        if (!purchaseRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Transaction not found"));
        }
        purchaseRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("deletedId", id));
    }

    /**
     * PUT update transaction
     */
    @PutMapping("/transactions/{id}")
    public ResponseEntity<?> updateTransaction(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Purchase t = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (body.containsKey("status"))
            t.setStatus(String.valueOf(body.get("status")));

        if (body.containsKey("productName"))
            t.setProductName(body.get("productName") != null ? String.valueOf(body.get("productName")) : null);

        if (body.containsKey("price")) {
            try {
                t.setPrice(Double.parseDouble(String.valueOf(body.get("price"))));
            } catch (Exception ignored) {}
        }

        if (body.containsKey("address"))
            t.setAddress(body.get("address") != null ? String.valueOf(body.get("address")) : null);

        return ResponseEntity.ok(purchaseRepository.save(t));
    }

    /**
     * 🟢 GET /transactions?userId=xxx
     * ❗ĐÃ BỎ CHECK ADMIN → Trả dữ liệu trực tiếp
     */
    @GetMapping("/transactions")
    public ResponseEntity<?> listTransactions(@RequestParam("userId") Long userId) {
        List<Purchase> txs = purchaseRepository.findAll();

        List<Map<String, Object>> out = new ArrayList<>();
        for (Purchase t : txs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("buyerId", t.getUserId());
            item.put("sellerId", t.getSellerId());
            item.put("productId", t.getProductId());
            item.put("productName", t.getProductName());
            item.put("productImage", t.getProductImage());
            item.put("price", t.getPrice());
            item.put("status", t.getStatus());
            item.put("fullName", t.getFullName());
            item.put("phone", t.getPhone());
            item.put("email", t.getEmail());
            item.put("address", t.getAddress());
            item.put("createdAt", t.getCreatedAt());
            item.put("complaintCount", complaintRepository.countByPurchaseId(t.getId()));
            out.add(item);
        }

        return ResponseEntity.ok(out);
    }

    /**
     * GET all complaints
     * ❗Không check ADMIN nữa
     */
    @GetMapping("/complaints")
    public ResponseEntity<?> adminListComplaints(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(
                complaintRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    /**
     * GET complaints by purchase
     * ❗Không check quyền admin / seller nữa
     */
    @GetMapping("/complaints/purchase/{purchaseId}")
    public ResponseEntity<?> getComplaintsForPurchase(
            @PathVariable Long purchaseId,
            @RequestParam("userId") Long userId
    ) {
        return ResponseEntity.ok(
                complaintRepository.findByPurchaseIdOrderByCreatedAtDesc(purchaseId)
        );
    }

    /**
     * GET complaint by ID
     * ❗Không check quyền nữa
     */
    @GetMapping("/complaints/{id}")
    public ResponseEntity<?> getComplaint(
            @PathVariable Long id,
            @RequestParam("userId") Long userId
    ) {
        Complaint c = complaintRepository.findById(id).orElse(null);
        if (c == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Complaint not found"));
        }
        return ResponseEntity.ok(c);
    }

    /**
     * PUT update complaint (admin reply)
     * ❗KHÔNG KIỂM TRA ADMIN NỮA
     */
    @PutMapping("/complaints/{id}")
    public ResponseEntity<?> updateComplaintStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        Long adminUserId = body.containsKey("adminUserId")
                ? Long.valueOf(String.valueOf(body.get("adminUserId")))
                : null;

        Complaint c = complaintRepository.findById(id).orElse(null);
        if (c == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Complaint not found"));

        if (body.containsKey("status"))
            c.setStatus(String.valueOf(body.get("status")));

        if (body.containsKey("adminResponse")) {
            String adminResp = String.valueOf(body.get("adminResponse"));
            c.setAdminResponse(adminResp);
            c.setAdminUserId(adminUserId);
            c.setRepliedAt(java.time.LocalDateTime.now());
            if (!body.containsKey("status")) c.setStatus("RESOLVED");

            // Save admin message
            ComplaintMessage msg = ComplaintMessage.builder()
                    .complaintId(c.getId())
                    .sender("ADMIN")
                    .senderName("Admin#" + adminUserId)
                    .content(adminResp)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            ComplaintMessage savedMsg = complaintMessageRepository.save(msg);

            // MQ push
            Long userId = purchaseRepository.findById(c.getPurchaseId())
                    .map(Purchase::getUserId)
                    .orElse(null);

            Map<String, Object> pay = new HashMap<>();
            pay.put("type", "complaint.message");
            pay.put("complaintId", c.getId());
            pay.put("messageId", savedMsg.getId());
            pay.put("purchaseId", c.getPurchaseId());
            pay.put("userId", userId);
            pay.put("senderName", msg.getSenderName());
            pay.put("content", adminResp);
            pay.put("createdAt", msg.getCreatedAt().toString());

            try { mqPublisher.publishToUser(pay); } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(complaintRepository.save(c));
    }

    /**
     * Manual sync
     */
    @PostMapping("/sync-purchases")
    public ResponseEntity<?> syncPurchases() {
        try {
            List<Purchase> synced = purchaseSyncService.syncAllPurchases();
            return ResponseEntity.ok(Map.of("syncedCount", synced.size()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    //API dành cho Seller (edit_news.html) để lấy thông tin người mua
    @GetMapping("/purchase/by-seller-listing/{productId}")
    public ResponseEntity<?> getPurchaseBySellerListing(
            @PathVariable Long productId,
            @RequestParam Long sellerId
    ) {
        // 1. Tìm đơn hàng có chứa productId này
        Purchase purchase = purchaseRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có đơn hàng nào cho sản phẩm này"));

        // 2. Bảo mật: Kiểm tra xem người đang hỏi (sellerId) có đúng là người bán đơn hàng này không
        if (purchase.getSellerId() == null || !purchase.getSellerId().equals(sellerId)) {
            // Nếu không phải người bán -> Trả về 404 để giấu thông tin
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Không tìm thấy giao dịch cho người bán này"));
        }

        // 3. Trả về thông tin Purchase
        return ResponseEntity.ok(purchase);
    }

}
