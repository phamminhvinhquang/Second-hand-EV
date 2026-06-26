package com.example.revenue_service.controller;

import com.example.revenue_service.service.RevenueService;
import com.example.revenue_service.service.RevenueSyncService; // <-- GIỮ LẠI DÒNG NÀY
import com.example.revenue_service.dto.RevenueDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/revenues")
@RequiredArgsConstructor
@Slf4j 
public class RevenueController {

    private final RevenueService revenueService;
    private final RestTemplate restTemplate;
    private final RevenueSyncService revenueSyncService; // <-- Đã được giải quyết

    @Value("${user.service.url:http://localhost:8084}")
    private String userServiceUrl;

    @PostMapping("/sync-now")
    public ResponseEntity<?> syncNow(@RequestParam(value = "userId") Long userId) {
        if (userId == null || !isAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Truy cập bị từ chối", "message", "Tài khoản của bạn không có quyền Admin."));
        }

        try {
            int importedCount = revenueSyncService.syncAllPurchases();
            return ResponseEntity.ok(Map.of("message", "Sync started/completed.", "importedCount", importedCount));
        } catch (Exception e) {
            log.error("Manual sync failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Manual sync failed", "detail", e.getMessage()));
        }
    }

    /**
     * Endpoint API lấy dữ liệu thống kê doanh thu.
     * URL: GET /api/revenues/stats?userId=...
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getRevenueStats(@RequestParam(value = "userId", required = false) Long userId) {
        // Kiểm tra quyền Admin 
        if (userId == null || !isAdmin(userId)) {
            log.warn("Truy cập API bị từ chối: userId={}", userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Truy cập bị từ chối", "message", "Không có quyền Admin."));
        }

        log.info("Truy cập API stats được chấp nhận cho Admin userId={}", userId);
        
        try {
            // Lấy dữ liệu
            List<RevenueDTO> daily = revenueService.getDailyRevenue();
            List<RevenueDTO> monthly = revenueService.getMonthlyRevenue();
            List<RevenueDTO> yearly = revenueService.getYearlyRevenue();

            // Log kiểm tra dữ liệu có null không (tránh lỗi Map.of)
            if (daily == null) daily = List.of();
            if (monthly == null) monthly = List.of();
            if (yearly == null) yearly = List.of();

            Map<String, List<RevenueDTO>> stats = Map.of(
                "dailyList", daily, 
                "monthlyList", monthly,
                "yearlyList", yearly
            );
            
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            // 🔥 QUAN TRỌNG: In lỗi ra console để debug
            log.error("Lỗi 500 khi lấy thống kê doanh thu: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal Server Error", "detail", e.getMessage()));
        }
    }

    // Hàm isAdmin giữ nguyên
    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        try {
            String rolesUrl = String.format("%s/api/user/%d/roles", userServiceUrl, userId);
            
            ResponseEntity<List<String>> rolesResp = restTemplate.exchange(
                    rolesUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );

            if (!rolesResp.getStatusCode().is2xxSuccessful() || rolesResp.getBody() == null) {
                log.warn("Kiểm tra quyền Admin thất bại (HTTP {}).", rolesResp.getStatusCode());
                return false;
            }
            
            List<String> roles = rolesResp.getBody().stream()
                    .filter(Objects::nonNull)
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());

            return roles.stream().anyMatch(r -> "ADMIN".equals(r));

        } catch (Exception e) {
            log.error("Lỗi khi gọi User Service để kiểm tra quyền Admin cho userId={}: {}", userId, e.getMessage());
            return false;
        }
    }
}