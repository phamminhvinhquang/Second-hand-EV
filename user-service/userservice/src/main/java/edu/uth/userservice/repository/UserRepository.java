package edu.uth.userservice.repository;

import edu.uth.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {
    
    // Các hàm cũ của bạn
    Optional<User> findByEmail(String email);
    Optional<User> findFirstByPhone(String phone);

    // --- BỔ SUNG CÁC HÀM JOIN FETCH CÒN THIẾU ---
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.phone = :phone")
    Optional<User> findByPhoneWithRoles(@Param("phone") String phone);

    // --- HẾT PHẦN BỔ SUNG ---

    // Hàm này đã có, dùng cho UserController
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.userId = :id")
    Optional<User> findByIdWithRoles(@Param("id") Integer id);
}