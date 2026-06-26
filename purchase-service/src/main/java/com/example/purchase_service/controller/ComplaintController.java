package com.example.purchase_service.controller;

import com.example.purchase_service.dto.CreateComplaintRequest;
import com.example.purchase_service.model.Complaint;
import com.example.purchase_service.model.ComplaintMessage;
import com.example.purchase_service.model.Purchase;
import com.example.purchase_service.mq.ComplaintMqPublisher;
import com.example.purchase_service.repository.ComplaintMessageRepository;
import com.example.purchase_service.repository.ComplaintRepository;
import com.example.purchase_service.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@Slf4j
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final PurchaseRepository purchaseRepository;
    private final ComplaintMessageRepository complaintMessageRepository;
    private final ComplaintMqPublisher mqPublisher;

    /**
     * Create a user complaint. This endpoint:
     *  - validates input,
     *  - ensures the referenced Purchase exists and belongs to someone (basic check),
     *  - saves Complaint and the first ComplaintMessage,
     *  - publishes an MQ event to notify admin(s).
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> createComplaint(@RequestBody CreateComplaintRequest req) {
        try {
            if (req == null || req.getPurchaseId() == null || req.getDetail() == null || req.getDetail().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "purchaseId and detail are required"));
            }

            Optional<Purchase> optP = purchaseRepository.findById(req.getPurchaseId());
            if (optP.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Purchase not found"));
            }
            Purchase p = optP.get();

            // Build complaint
            Complaint c = Complaint.builder()
                    .purchaseId(req.getPurchaseId())
                    .senderName(req.getSenderName())
                    .senderPhone(req.getSenderPhone())
                    .senderEmail(req.getSenderEmail())
                    .detail(req.getDetail())
                    .status("NEW")
                    .createdAt(LocalDateTime.now())
                    .build();

            Complaint saved = complaintRepository.save(c);

            // Save initial message (user message)
            ComplaintMessage msg = ComplaintMessage.builder()
                    .complaintId(saved.getId())
                    .sender("USER")
                    .senderName(req.getSenderName() == null ? "" : req.getSenderName())
                    .content(req.getDetail())
                    .createdAt(LocalDateTime.now())
                    .build();
            ComplaintMessage savedMsg = complaintMessageRepository.save(msg);

            // Publish to admin via MQ. We include purchaseId so admin side can route/display.
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "complaint.message");
            payload.put("complaintId", saved.getId());
            payload.put("messageId", savedMsg.getId());
            payload.put("purchaseId", saved.getPurchaseId());
            payload.put("userId", p.getUserId());
            payload.put("senderName", req.getSenderName());
            payload.put("content", req.getDetail());
            payload.put("createdAt", savedMsg.getCreatedAt().toString());

            try {
                mqPublisher.publishToAdmin(payload);
            } catch (Exception mqEx) {
                // Log MQ errors but do not fail the request; admin notification may be retried by infra.
                log.error("Failed to publish complaint to admin MQ: {}", mqEx.getMessage(), mqEx);
            }

            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            log.error("Error creating complaint", ex);
            // Return a helpful error (avoid exposing stacktrace)
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error", "detail", ex.getMessage()));
        }
    }

    /**
     * Public: get complaints for a purchase (user must provide their userId for basic access check)
     */
    @GetMapping("/purchase/{purchaseId}")
    public ResponseEntity<?> getComplaintsForPurchasePublic(@PathVariable("purchaseId") Long purchaseId,
                                                            @RequestParam("userId") Long userId) {
        if (purchaseId == null || userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "purchaseId and userId required"));
        }
        Purchase p = purchaseRepository.findById(purchaseId).orElse(null);
        if (p == null) return ResponseEntity.status(404).body(Map.of("error", "Purchase not found"));

        if (p.getUserId() == null || !p.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        var list = complaintRepository.findByPurchaseIdOrderByCreatedAtDesc(purchaseId);
        return ResponseEntity.ok(list);
    }
}
