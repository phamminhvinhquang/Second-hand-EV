package edu.uth.userservice.controller;

import edu.uth.userservice.dto.LoginRequest;
import edu.uth.userservice.dto.LoginResponse;
import edu.uth.userservice.dto.RegisterRequest;
import edu.uth.userservice.dto.UserDTO;
import edu.uth.userservice.model.User;
import edu.uth.userservice.security.JwtUtil;
import edu.uth.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
//@CrossOrigin(origins = {"http://127.0.0.1:5501", "http://localhost:3000", "http://localhost:5501"})
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * ✅ Register: tạo user rồi trả token (auto-login).
     * Trả 201 Created + Location + Authorization header + body LoginResponse
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if ((req.getEmail() == null || req.getEmail().isBlank()) &&
                (req.getPhone() == null || req.getPhone().isBlank())) {
            return ResponseEntity.badRequest().body("Email or phone is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Password is required");
        }

        // Check uniqueness
        if (req.getEmail() != null && !req.getEmail().isBlank() &&
                userService.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        if (req.getPhone() != null && !req.getPhone().isBlank() &&
                userService.findByPhone(req.getPhone()).isPresent()) {
            return ResponseEntity.badRequest().body("Phone already exists");
        }

        // Build user
        User u = new User();
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        u.setPassword(req.getPassword());
        u.setAddress(req.getAddress());
        u.setAccountStatus("active");

        User saved = userService.register(u);

        String subject = (saved.getEmail() != null && !saved.getEmail().isBlank())
                ? saved.getEmail()
                : (saved.getPhone() != null ? saved.getPhone() : ("user-" + saved.getUserId()));

        // ✅ Lấy roles
        Set<String> roles = saved.getRoles()
                .stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());

        // ✅ Gọi generateToken mới
        String token = jwtUtil.generateToken(subject, saved.getUserId(), roles);

        LoginResponse resp = new LoginResponse(token, new UserDTO(saved));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setLocation(URI.create("/api/user/" + saved.getUserId()));

        return ResponseEntity.created(URI.create("/api/user/" + saved.getUserId()))
                .headers(headers)
                .body(resp);
    }

    /**
     * ✅ Login: xác thực user rồi trả token có roles
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        String id = Optional.ofNullable(req.getIdentifier())
                .filter(s -> !s.isBlank())
                .or(() -> Optional.ofNullable(req.getEmail()).filter(s -> !s.isBlank()))
                .orElse(null);

        if (id == null || id.isBlank() || req.getPassword() == null || req.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Identifier and password required");
        }

        Optional<User> opt;
        if (id.contains("@")) {
           opt = userService.findByEmailWithRoles(id).or(() -> userService.findByPhoneWithRoles(id));
        } else {
            opt = userService.findByEmail(id).or(() -> userService.findByPhone(id));
        }

        if (opt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        User user = opt.get();

        if (!"active".equalsIgnoreCase(user.getAccountStatus())) {
            return ResponseEntity.status(403).body("Account not active");
        }

        boolean ok = userService.checkPassword(req.getPassword(), user.getPassword());
        if (!ok) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String subject = (user.getEmail() != null && !user.getEmail().isBlank())
                ? user.getEmail()
                : (user.getPhone() != null ? user.getPhone() : ("user-" + user.getUserId()));

        // ✅ Lấy roles cho user
        Set<String> roles = user.getRoles()
                .stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());

        // ✅ Sinh token với roles
        String token = jwtUtil.generateToken(subject, user.getUserId(), roles);

        LoginResponse resp = new LoginResponse(token, new UserDTO(user));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        return ResponseEntity.ok().headers(headers).body(resp);
    }
}
