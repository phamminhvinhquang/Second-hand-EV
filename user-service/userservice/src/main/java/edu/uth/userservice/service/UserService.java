package edu.uth.userservice.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uth.userservice.model.Role;
import edu.uth.userservice.model.User;
import edu.uth.userservice.mq.MQPublisher;
import edu.uth.userservice.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MQPublisher publisher; // ✅ để gửi MQ event sang wallet-service

    /**
     * Tìm user theo email
     */
    public Optional<User> findByEmail(String email) {
        return repo.findByEmail(email);
    }

    /**
     * Tìm user theo phone
     */
    public Optional<User> findByPhone(String phone) {
        return repo.findFirstByPhone(phone);
    }

    /**
     * Tìm user theo id (Integer)
     */
    public Optional<User> findById(Integer id) {
        return repo.findById(id);
    }

    /**
     * Đăng ký user: hash password, gán role mặc định, gửi MQ event tạo ví.
     */
    @Transactional
    public User register(User user) {
        // hash password trước khi lưu
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getAccountStatus() == null) user.setAccountStatus("active");

        // Lưu user để lấy ID
        User saved = repo.save(user);

        // Gán role mặc định USER
        Role userRole = roleService.findByName("USER").orElse(null);
        if (userRole != null) {
            saved.getRoles().add(userRole);
            saved = repo.save(saved);
        }

        // ⬇️ =======================================================
        // ✅ [ĐÃ SỬA] Gửi message sang wallet-service
        // Truyền thẳng đối tượng 'saved' (User) thay vì Map
        publisher.publish("user.created", saved);
        // ⬆️ =======================================================

        return saved;
    }

    /**
     * Kiểm tra mật khẩu thô (raw) so với hashed lưu trong DB
     */
    public boolean checkPassword(String rawPassword, String hashed) {
        return passwordEncoder.matches(rawPassword, hashed);
    }

    @Transactional
    public User updateProfile(Integer userId,
                              String name,
                              String email,
                              String phone,
                              String address
                            ) throws IllegalArgumentException {
        Optional<User> opt = repo.findById(userId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User u = opt.get();

        if (name != null) u.setName(name);
        if (email != null) u.setEmail(email.trim().toLowerCase());
        if (phone != null) u.setPhone(phone);
        if (address != null) u.setAddress(address);
        
        // Sửa ở đây: Lưu vào biến thay vì return ngay
        User updatedUser = repo.save(u);

        // ⬇️ =======================================================
        // ✅ [BỔ SUNG] Gửi message khi cập nhật profile
        //    (Gửi đối tượng User model đã được cập nhật)
        publisher.publish("user.updated", updatedUser);
        // ⬆️ =======================================================

        return updatedUser;
    }
    
    @Transactional
    public void changePassword(Integer userId, String currentPassword, String newPassword) throws IllegalArgumentException {
        Optional<User> opt = repo.findById(userId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User u = opt.get();

        if (!checkPassword(currentPassword, u.getPassword())) {
            throw new IllegalArgumentException("Current password incorrect");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        repo.save(u);
    }

    /* ---------------- Role management ---------------- */

    @Transactional
    public User addRoleToUser(Integer userId, String roleName) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Role role = roleService.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        boolean added = user.getRoles().stream().noneMatch(r -> r.getName().equalsIgnoreCase(roleName));
        if (added) {
            user.getRoles().add(role);
            user = repo.save(user);

            // ⬇️ =======================================================
            // ✅ [ĐÃ SỬA] Nếu role là STAFF thì gửi event MQ
            if ("STAFF".equalsIgnoreCase(roleName)) {
                Map<String, Object> event = Map.of(
                        "userId", user.getUserId(),
                        "role", "STAFF",
                        "eventType", "ADD"
                );
                publisher.publish("user.role.updated", event);
            }
            // ⬆️ =======================================================
        }

        return user;
    }


    @Transactional
    public User removeRoleFromUser(Integer userId, String roleName) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean removed = user.getRoles().removeIf(r -> r.getName().equalsIgnoreCase(roleName));
        if (removed) {
            user = repo.save(user);

            // ⬇️ =======================================================
            // ✅ [ĐÃ SỬA] Nếu bỏ role STAFF thì gửi event
            if ("STAFF".equalsIgnoreCase(roleName)) {
                Map<String, Object> event = Map.of(
                        "userId", user.getUserId(),
                        "role", "STAFF",
                        "eventType", "REMOVE"
                );
                publisher.publish("user.role.updated", event);
            }
            // ⬆️ =======================================================
        }

        return user;
    }


    @Transactional
    public User setRolesForUser(Integer userId, List<String> roleNames) {
        User user = repo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<Role> newRoles = roleNames == null ? new HashSet<>() :
                roleNames.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .map(rn -> roleService.findByName(rn)
                                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + rn)))
                        .collect(Collectors.toSet());

        user.setRoles(newRoles);
        return repo.save(user);
    }

    public List<User> findAllUsers() {
        return repo.findAll();
    }

    public List<User> listAllUsers() {
        return repo.findAll();
    }

    public Set<String> getRoleNamesForUser(Integer userId) {
        return repo.findById(userId)
                .map(u -> u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }

    @Transactional
    public User setAccountStatus(Integer userId, String status) {
        User u = repo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.setAccountStatus(status);
        return repo.save(u);
    }

    @Transactional
    public User lockUser(Integer userId) {
        return setAccountStatus(userId, "locked");
    }

    @Transactional
    public User unlockUser(Integer userId) {
        return setAccountStatus(userId, "active");
    }

    // --- BỔ SUNG CHO AUTHCONTROLLER ---

    @Transactional(readOnly = true)
    public Optional<User> findByEmailWithRoles(String email) {
        return repo.findByEmailWithRoles(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByPhoneWithRoles(String phone) {
        return repo.findByPhoneWithRoles(phone);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByIdWithRoles(Integer id) {
        return repo.findByIdWithRoles(id);
    }
    @Transactional
    public User processOAuthPostLogin(Map<String, Object> attributes, String registrationId) {

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email not found in OAuth2 attributes");
        }

        // Tìm user đã tồn tại
        Optional<User> opt = this.findByEmail(email);

        if (opt.isPresent()) {
            // ❗ Không cập nhật name nữa – giữ nguyên dữ liệu mà user đã chỉnh trong profile
            return opt.get();
        }

        // 🆕 Lần đầu login -> tạo user mới
        User user = new User();
        user.setEmail(email);
        user.setName(name == null ? "OAuth User" : name);
        user.setPassword(passwordEncoder.encode("OAuth2_Generated_Password_" + UUID.randomUUID()));
        user.setAccountStatus("active");

        // Gán role USER lần đầu
        Role userRole = roleService.findByName("USER")
                .orElseGet(() -> roleService.save(new Role("USER")));
        user.getRoles().add(userRole);

        User saved = repo.save(user);

        // Gửi sự kiện tạo mới ví
        publisher.publish("user.created", saved);

        return saved;
    }

}