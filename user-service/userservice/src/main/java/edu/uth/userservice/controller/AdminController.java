package edu.uth.userservice.controller;

import edu.uth.userservice.dto.RoleRequest;
import edu.uth.userservice.dto.RolesUpdateRequest;
import edu.uth.userservice.dto.UserDTO;
import edu.uth.userservice.model.User;
import edu.uth.userservice.service.UserService;
import edu.uth.userservice.service.RoleService;
import edu.uth.userservice.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
//@CrossOrigin(origins = {"http://127.0.0.1:5501","http://localhost:3000","http://localhost:5501"})
public class AdminController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    // helper: lấy userId từ Authorization header (Bearer token)
    private Integer getUserIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }

    // helper: kiểm tra caller có role ADMIN không
    private boolean callerIsAdmin(String authHeader) {
        Integer callerId = getUserIdFromHeader(authHeader);
        if (callerId == null) return false;
        Set<String> roles = userService.getRoleNamesForUser(callerId);
        // ✅ Cập nhật: SUPER_ADMIN cũng là ADMIN
        return roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r) || "SUPER_ADMIN".equalsIgnoreCase(r));
    }

    /** GET /api/admin/users => trả danh sách user (basic) */
    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestHeader(value="Authorization", required=false) String authHeader) {
        if (!callerIsAdmin(authHeader)) return ResponseEntity.status(403).body("Forbidden");

        List<User> users = userService.findAllUsers(); // we'll add this method to UserService (see note)
        List<Map<String, Object>> out = users.stream().map(u -> {
            Map<String,Object> m = new HashMap<>();
            m.put("userId", u.getUserId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
             m.put("accountStatus", u.getAccountStatus()); // ví dụ "active" hoặc "locked"
            m.put("roles", u.getRoles() == null ? Collections.emptySet() :
                    u.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()));
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    // POST /api/admin/users/{id}/lock
    @PostMapping("/users/{id}/lock")
    public ResponseEntity<?> lockUser(
            @RequestHeader(value="Authorization", required=false) String authHeader,
            @PathVariable("id") Integer id) {
        if (!callerIsAdmin(authHeader)) return ResponseEntity.status(403).body("Forbidden");
        // ===== 🛑 BẢO VỆ SUPERADMIN 🛑 =====
        // Lấy vai trò của user mục tiêu (người bị khóa)
        Set<String> targetRoles = userService.getRoleNamesForUser(id);
        if (targetRoles.contains("SUPER_ADMIN")) {
            return ResponseEntity.status(403).body("Không thể tác động đến tài khoản Super Admin.");
        }
        // ======================================
        try {
            User updated = userService.lockUser(id);
            Map<String,Object> resp = Map.of(
                "userId", updated.getUserId(),
                "accountStatus", updated.getAccountStatus()
            );
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // POST /api/admin/users/{id}/unlock
    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<?> unlockUser(
            @RequestHeader(value="Authorization", required=false) String authHeader,
            @PathVariable("id") Integer id) {
        if (!callerIsAdmin(authHeader)) return ResponseEntity.status(403).body("Forbidden");
        // ===== 🛑 BẢO VỆ SUPERADMIN 🛑 (2) =====
        Set<String> targetRoles = userService.getRoleNamesForUser(id);
        if (targetRoles.contains("SUPER_ADMIN")) {
            return ResponseEntity.status(403).body("Không thể tác động đến tài khoản Super Admin.");
        }
        // ======================================
        try {
            User updated = userService.unlockUser(id);
            Map<String,Object> resp = Map.of(
                "userId", updated.getUserId(),
                "accountStatus", updated.getAccountStatus()
            );
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /** GET roles for a specific user */
    @GetMapping("/users/{id}/roles")
    public ResponseEntity<?> getUserRoles(
            @RequestHeader(value="Authorization", required=false) String authHeader,
            @PathVariable("id") Integer id) {
        if (!callerIsAdmin(authHeader)) return ResponseEntity.status(403).body("Forbidden");

        Set<String> roles = userService.getRoleNamesForUser(id);
        return ResponseEntity.ok(roles);
    }

    /** POST add one role to user */
    @PostMapping("/users/{id}/roles")
    public ResponseEntity<?> addRoleToUser(
            @RequestHeader(value="Authorization", required=false) String authHeader,
            @PathVariable("id") Integer id,
            @RequestBody RoleRequest req) {
        if (!callerIsAdmin(authHeader)) return ResponseEntity.status(403).body("Forbidden");
        if (req == null || req.getRole() == null || req.getRole().isBlank()) {
            return ResponseEntity.badRequest().body("role required");
        }
        // ===== 🛑 BẢO VỆ SUPERADMIN 🛑 (3) =====
        Set<String> targetRoles = userService.getRoleNamesForUser(id);
        if (targetRoles.contains("SUPER_ADMIN")) {
            return ResponseEntity.status(403).body("Không thể tác động đến tài khoản Super Admin.");
        }
        // ======================================
        try {
            User updated = userService.addRoleToUser(id, req.getRole().trim().toUpperCase());
            return ResponseEntity.ok(new UserDTO(updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /** PUT set roles (replace) */
    @PutMapping("/users/{id}/roles")
    public ResponseEntity<?> setRolesForUser(
            @RequestHeader(value="Authorization", required=false) String authHeader,
            @PathVariable("id") Integer id,
            @RequestBody RolesUpdateRequest req) {
        if (!callerIsAdmin(authHeader)) return ResponseEntity.status(403).body("Forbidden");
        // ===== 🛑 BẢO VỆ SUPERADMIN 🛑 (4) =====
        Set<String> targetRoles = userService.getRoleNamesForUser(id);
        if (targetRoles.contains("SUPER_ADMIN")) {
            return ResponseEntity.status(403).body("Không thể tác động đến tài khoản Super Admin.");
        }
        // ======================================
        try {
            List<String> roles = req == null ? Collections.emptyList() : req.getRoles();
            User updated = userService.setRolesForUser(id, roles);
            return ResponseEntity.ok(new UserDTO(updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /** DELETE role from user */
    @DeleteMapping("/users/{id}/roles/{role}")
    public ResponseEntity<?> removeRoleFromUser(
            @RequestHeader(value="Authorization", required=false) String authHeader,
            @PathVariable("id") Integer id,
            @PathVariable("role") String role) {
        if (!callerIsAdmin(authHeader)) return ResponseEntity.status(403).body("Forbidden");
        // ===== 🛑 BẢO VỆ SUPERADMIN 🛑 (5) =====
        Set<String> targetRoles = userService.getRoleNamesForUser(id);
        if (targetRoles.contains("SUPER_ADMIN")) {
            // Đặc biệt: Cho phép Super Admin tự xóa vai trò của chính mình (nếu muốn)
            // Nhưng vẫn cấm Admin khác xóa vai trò của Super Admin
            Integer callerId = getUserIdFromHeader(authHeader);
            if (callerId == null || !callerId.equals(id)) {
                return ResponseEntity.status(403).body("Không thể tác động đến tài khoản Super Admin.");
            }
        }
        try {
            User updated = userService.removeRoleFromUser(id, role);
            return ResponseEntity.ok(new UserDTO(updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
    
}
