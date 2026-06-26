package com.example.revenue_service.controller;

import com.example.revenue_service.dto.PurchaseDTO;
import com.example.revenue_service.service.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import_revenues") 
@RequiredArgsConstructor
@Slf4j
public class RevenueImportController {

    private final RevenueService revenueService;
    
    // Sử dụng ObjectMapper được cấu hình để xử lý Date/Time nếu cần
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * POST /api/import_revenues/import
     * Nhận dữ liệu PurchaseDTO (một object hoặc một array) và lưu vào bảng Revenue.
     * Dùng cho mục đích đồng bộ dữ liệu giao dịch từ Purchase Service.
     */
    @PostMapping("/import")
    public ResponseEntity<?> importPurchases(@RequestBody Object body) {
        try {
            if (body == null) return ResponseEntity.badRequest().body(Map.of("error", "empty body"));

            List<PurchaseDTO> purchases;

            if (body instanceof List) {
                // Xử lý List<Map<String, Object>> -> List<PurchaseDTO>
                List<?> raw = (List<?>) body;
                purchases = objectMapper.convertValue(
                        raw, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, PurchaseDTO.class)
                );
            } else {
                // Xử lý single object (Map<String, Object>) -> PurchaseDTO
                PurchaseDTO p = objectMapper.convertValue(body, PurchaseDTO.class);
                if (p.getId() == null) return ResponseEntity.badRequest().body(Map.of("error", "purchase.id required"));
                purchases = List.of(p);
            }

            int created = revenueService.importPurchases(purchases);
            return ResponseEntity.ok(Map.of(
                    "imported", created, 
                    "requested", purchases.size(), 
                    "message", "Import process completed."
            ));

        } catch (Exception ex) {
            log.error("Import failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "import failed", "detail", ex.getMessage()));
        }
    }
}