package com.example.like_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.like_service.model.Like;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByProductIdAndUserId(Long productId, Long userId);
    boolean existsByProductIdAndUserId(Long productId, Long userId);
    void deleteByProductIdAndUserId(Long productId, Long userId);

    // Lấy danh sách like của 1 user (mới nhất trước)
    List<Like> findByUserIdOrderByIdDesc(Long userId);

    List<Like> findByProductId(Long productId);
}
