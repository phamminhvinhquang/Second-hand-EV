package com.example.cart_service.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.cart_service.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // Kiểm tra product đã có trong giỏ của user chưa
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    // Lấy các item theo userId (sắp xếp mới nhất trước)
    List<Cart> findByUserIdOrderByIdDesc(Long userId);

    // Xóa TẤT CẢ cart chứa productId này
    @Modifying
    @Transactional
    @Query("DELETE FROM Cart c WHERE c.productId = :productId")
    int deleteByProductId(@Param("productId") Long productId);

    // ✅ Xóa toàn bộ giỏ hàng của 1 user (dùng khi thanh toán xong)
    @Modifying
    @Transactional
    @Query("DELETE FROM Cart c WHERE c.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    // Lấy danh sách tất cả productId duy nhất trong bảng carts
    @Query("SELECT DISTINCT c.productId FROM Cart c WHERE c.productId IS NOT NULL")
    Set<Long> findAllDistinctProductIds();
}
