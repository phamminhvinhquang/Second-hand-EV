package com.example.revenue_service.service;

import com.example.revenue_service.dto.RevenueDTO;
import com.example.revenue_service.dto.PurchaseDTO;
import com.example.revenue_service.model.Revenue;
import com.example.revenue_service.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueService {

    private final RevenueRepository revenueRepository;

    // ... (Giữ nguyên các hàm mapToStatDTO, getDailyRevenue, v.v...)
    private RevenueDTO mapToStatDTO(Map<String, Object> map) {
        Double total = 0.0;
        Object totalObj = map.get("total");
        if (totalObj instanceof BigDecimal) total = ((BigDecimal) totalObj).doubleValue();
        else if (totalObj instanceof Number) total = ((Number) totalObj).doubleValue();

        Integer year = map.get("year") instanceof Number ? ((Number) map.get("year")).intValue() : null;
        Integer month = map.get("month") instanceof Number ? ((Number) map.get("month")).intValue() : null;
        String date = map.get("date") instanceof String ? (String) map.get("date") : null;
        String label = (year != null && month != null) ? String.format("%d-%02d", year, month) : date;

        return RevenueDTO.builder().date(date).year(year).month(month).label(label).total(total).build();
    }

    public List<RevenueDTO> getDailyRevenue() {
        List<Map<String, Object>> results = revenueRepository.findDailyRevenue();
        return results.stream().map(this::mapToStatDTO).collect(Collectors.toList());
    }
    public List<RevenueDTO> getMonthlyRevenue() {
        List<Map<String, Object>> results = revenueRepository.findMonthlyRevenue();
        return results.stream().map(this::mapToStatDTO).collect(Collectors.toList());
    }
    public List<RevenueDTO> getYearlyRevenue() {
        List<Map<String, Object>> results = revenueRepository.findYearlyRevenue();
        return results.stream().map(this::mapToStatDTO).collect(Collectors.toList());
    }

    /**
     * Xử lý import: Thêm (nếu completed) hoặc Xóa (nếu returned)
     */
    @Transactional
    public Revenue importFromPurchase(PurchaseDTO p) {
        if (p == null || p.getId() == null) return null;

        String status = p.getStatus() != null ? p.getStatus().toLowerCase() : "";
        Long purchaseId = p.getId();

        // 🔴 TRƯỜNG HỢP 1: TRẢ HÀNG -> XÓA DOANH THU
        if ("returned".equals(status) || "cancelled".equals(status)) {
            if (revenueRepository.existsByPurchaseId(purchaseId)) {
                revenueRepository.deleteByPurchaseId(purchaseId);
                log.info("❌ Deleted Revenue for Returned Purchase ID={}", purchaseId);
            } else {
                log.info("ℹ️ Ignored Return for Purchase ID={} (Not found in revenue)", purchaseId);
            }
            return null;
        }

        // 🟢 TRƯỜNG HỢP 2: HOÀN THÀNH -> CỘNG DOANH THU
        if ("completed".equals(status)) {
            if (revenueRepository.existsByPurchaseId(purchaseId)) {
                log.info("Revenue for Purchase ID={} already exists, skipping.", purchaseId);
                return null;
            }

            if (p.getPrice() == null || p.getPrice() <= 0) return null;

            // Parse date logic
            LocalDate revenueDate = LocalDate.now();
            try {
                if (p.getCreatedAt() != null) revenueDate = LocalDateTime.parse(p.getCreatedAt()).toLocalDate();
            } catch (Exception ignored) {}

            Revenue r = Revenue.builder()
                    .purchaseId(purchaseId)
                    .sellerId(p.getSellerId())
                    .amount(p.getPrice())
                    .revenueDate(revenueDate)
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("✅ Imported Revenue for Purchase ID={}, Amount={}", purchaseId, p.getPrice());
            return revenueRepository.save(r);
        }

        return null; // Các trạng thái khác (waiting_delivery...) bỏ qua
    }

    @Transactional
    public int importPurchases(List<PurchaseDTO> purchases) {
        if (purchases == null || purchases.isEmpty()) return 0;
        int count = 0;
        for (PurchaseDTO p : purchases) {
            try {
                importFromPurchase(p); 
                // Lưu ý: Hàm trên trả về null nếu xóa hoặc bỏ qua, nhưng logic đã chạy
                count++;
            } catch (Exception e) {
                log.error("Error processing purchase ID {}: {}", p.getId(), e.getMessage());
            }
        }
        return count;
    }
}