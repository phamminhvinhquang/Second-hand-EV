package edu.uth.userservice.controller;

import edu.uth.userservice.dto.ChangePasswordRequest;
import edu.uth.userservice.dto.TransactionHistoryDTO;
import edu.uth.userservice.dto.UpdateProfileRequest;
import edu.uth.userservice.dto.UserDTO;
import edu.uth.userservice.model.User;
import edu.uth.userservice.security.JwtUtil;
import edu.uth.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import edu.uth.userservice.model.TransactionHistory; // ⭐️ Import mới
import edu.uth.userservice.repository.TransactionHistoryRepository; // ⭐️ Import mới
import edu.uth.userservice.repository.UserRepository;

// ⭐️ Import mới
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
// @CrossOrigin(origins = {
//        // "http://127.0.0.1:5501",
//        // "http://localhost:3000",
//        // "http://localhost:5501",
//         "http://localhost:8089"  // ✅ cho phép wallet-service gọi trực tiếp
// })
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // ⭐️ 1. TIÊM REPO MỚI CỦA BƯỚC 3
    @Autowired
    private TransactionHistoryRepository historyRepo;

    @Autowired
    private UserRepository userRepo;

    /** Helper: lấy userId từ token */
    private Integer getUserIdFromAuthHeader(String authHeader) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Integer) {
            return (Integer) auth.getPrincipal();
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        try {
            return jwtUtil.extractUserId(token);
        } catch (Exception ex) {
            return null;
        }
    }

    // ========================================
    // 🔹 LẤY THÔNG TIN USER HIỆN TẠI
    // ========================================
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Integer userId = getUserIdFromAuthHeader(authHeader);
        if (userId == null) return ResponseEntity.status(401).body("Unauthorized");

        Optional<User> opt = userService.findByIdWithRoles(userId);
        if (opt.isEmpty()) return ResponseEntity.status(404).body("User not found");

        return ResponseEntity.ok(new UserDTO(opt.get()));
    }

    // ========================================
    // 🔹 CẬP NHẬT PROFILE
    // ========================================
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateProfileRequest req) {

        Integer userId = getUserIdFromAuthHeader(authHeader);
        if (userId == null) return ResponseEntity.status(401).body("Unauthorized");

        String incomingEmail = req.getEmail() == null ? null : req.getEmail().trim().toLowerCase();
        String incomingPhone = req.getPhone() == null ? null : req.getPhone().trim();

        if (incomingEmail != null && !incomingEmail.isBlank()) {
            Optional<User> byEmail = userService.findByEmail(incomingEmail);
            if (byEmail.isPresent() && !Objects.equals(byEmail.get().getUserId(), userId)) {
                return ResponseEntity.badRequest().body("Email already in use");
            }
        }

        if (incomingPhone != null && !incomingPhone.isBlank()) {
            Optional<User> byPhone = userService.findByPhone(incomingPhone);
            if (byPhone.isPresent() && !Objects.equals(byPhone.get().getUserId(), userId)) {
                return ResponseEntity.badRequest().body("Phone already in use");
            }
        }

        try {
            User updated = userService.updateProfile(
                    userId,
                    req.getName(),
                    incomingEmail,
                    incomingPhone,
                    req.getAddress()
                    
            );

            Map<String, Object> body = new HashMap<>();
            body.put("user", new UserDTO(updated));
            return ResponseEntity.ok(body);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }

    // ========================================
    // 🔹 ĐỔI MẬT KHẨU
    // ========================================
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ChangePasswordRequest req) {

        Integer userId = getUserIdFromAuthHeader(authHeader);
        if (userId == null) return ResponseEntity.status(401).body("Unauthorized");

        if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()
                || req.getNewPassword() == null || req.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Both currentPassword and newPassword are required");
        }

        try {
            userService.changePassword(userId, req.getCurrentPassword(), req.getNewPassword());
            return ResponseEntity.ok("Password changed");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }

    // ========================================
    // 🔹 LẤY PROFILE USER THEO ID
    // ========================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfileById(@PathVariable("id") Integer id) {
        Optional<User> userOpt = userService.findByIdWithRoles(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new UserDTO(userOpt.get()));
    }

    // ========================================
    // 🔹 PUBLIC: LẤY DANH SÁCH ROLES CỦA USER
    // ========================================
    @GetMapping("/{id}/roles")
    public ResponseEntity<?> getUserRoles(@PathVariable("id") Integer id) {
        Optional<User> userOpt = userService.findByIdWithRoles(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        Set<String> roles = new HashSet<>();
        userOpt.get().getRoles().forEach(r -> roles.add(r.getName().toUpperCase()));

        log.info("✅ [UserService] GET /api/user/{}/roles -> {}", id, roles);
        return ResponseEntity.ok(roles);
    }
    // ========================================================
    // ⭐️ 2. ENDPOINT MỚI: LẤY LỊCH SỬ GIAO DỊCH CỦA TÔI
    // ========================================================
    @GetMapping("/me/history")
public ResponseEntity<?> getMyHistory(
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
    
    Integer userId = getUserIdFromAuthHeader(authHeader);
    if (userId == null) {
        return ResponseEntity.status(401).body("Unauthorized");
    }

    // 1. Lấy danh sách lịch sử gốc (Entity)
    List<TransactionHistory> histories = historyRepo.findByUserIdOrderByCreatedAtDesc(userId);
    
    // 2. Map sang DTO và điền tên người bán
    List<TransactionHistoryDTO> responseList = histories.stream().map(h -> {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        // Copy dữ liệu cơ bản
        dto.setId(h.getId());
        dto.setTransactionId(h.getTransactionId());
        dto.setAmount(h.getAmount());
        dto.setMethod(h.getMethod());
        dto.setType(h.getType());
        dto.setStatus(h.getStatus());
        dto.setCreatedAt(h.getCreatedAt());
        dto.setSellerId(h.getSellerId());
        dto.setUserId(h.getUserId());
        // 👇 THÊM 2 DÒNG NÀY 👇
        dto.setProductName(h.getProductName());
        dto.setProductImg(h.getProductImg());

        // ⭐️ LOGIC LẤY TÊN NGƯỜI BÁN TỪ CHÍNH USER-SERVICE ⭐️
        if (h.getSellerId() != null) {
            // Lưu ý: sellerId trong History là Long, userId trong User là Integer
            // Cần ép kiểu cho khớp
            System.out.println("👉 Đang tìm User ID: " + h.getSellerId());
            userRepo.findById(h.getSellerId().intValue()).ifPresent(seller -> {
                System.out.println("✅ Đã tìm thấy: " + seller.getName());
                dto.setSellerName(seller.getName());
            });
        } else {
            dto.setSellerName("Hệ thống"); // Hoặc null nếu là nạp tiền
        }

        return dto;
    }).toList();

    return ResponseEntity.ok(responseList);
}
}
