
package edu.uth.example.review_service.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; 

import edu.uth.example.review_service.Model.ReviewableTransaction;

public interface ReviewableTransactionRepository extends JpaRepository<ReviewableTransaction, Long> {
    
    Optional<ReviewableTransaction> findByProductId(Long productId);

    /**
     * Lấy các "vé" MÀ NGƯỜI DÙNG CHƯA ĐÁNH GIÁ (Cần làm)
     * Sắp xếp theo ngày hết hạn GẦN NHẤT
     */
    @EntityGraph(attributePaths = "reviews")
    @Query("SELECT t FROM ReviewableTransaction t WHERE " +
           "(t.sellerId = :userId AND t.isSellerReviewed = false) OR " +
           "(t.buyerId = :userId AND t.isBuyerReviewed = false) " +
           "ORDER BY t.expiresAt ASC") // Sắp xếp theo ngày hết hạn GẦN NHẤT
    Page<ReviewableTransaction> findTasksToReviewByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Lấy các "vé" MÀ NGƯỜI DÙNG ĐÃ ĐÁNH GIÁ (Hoàn tất)
     * Sắp xếp theo ngày hết hạn MỚI NHẤT
     */
    @EntityGraph(attributePaths = "reviews")
    @Query("SELECT t FROM ReviewableTransaction t WHERE " +
           "(t.sellerId = :userId AND t.isSellerReviewed = true) OR " +
           "(t.buyerId = :userId AND t.isBuyerReviewed = true) " +
           "ORDER BY t.expiresAt DESC")
    Page<ReviewableTransaction> findTasksCompletedByUserId(@Param("userId") Long userId, Pageable pageable);

    //  Đếm số lượng giao dịch chưa đánh giá
    @Query("SELECT COUNT(t) FROM ReviewableTransaction t WHERE " +
           "(t.sellerId = :userId AND t.isSellerReviewed = false) OR " +
           "(t.buyerId = :userId AND t.isBuyerReviewed = false)")
    Long countTasksToReview(@Param("userId") Long userId);
}