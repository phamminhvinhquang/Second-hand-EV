
package edu.uth.example.review_service.Repository;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uth.example.review_service.Model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  
    List<Review> findByReviewedPartyId(Long reviewedPartyId);

    
    @Query(value = "SELECT AVG(rating) AS avg_rating, COUNT(*) AS total_reviews FROM reviews WHERE reviewed_party_id = :userId", nativeQuery = true)
    List<Object[]> getReviewStatsForUser(@Param("userId") Long userId);


    @EntityGraph(attributePaths = {"transaction"})
    Page<Review> findByReviewedPartyId(Long reviewedPartyId, Pageable pageable);

  
    @EntityGraph(attributePaths = {"transaction"})
    Page<Review> findByReviewerId(Long reviewerId, Pageable pageable);
    
  
    /**
     * Tìm tất cả (không phân trang) các review DO một người dùng viết.
     * Dùng để cập nhật tên khi user đổi tên.
     */
    List<Review> findByReviewerId(Long reviewerId);
    

    // (Người viết review phải là Buyer trong transaction)
    @EntityGraph(attributePaths = {"transaction"})
    @Query("SELECT r FROM Review r JOIN r.transaction t WHERE r.reviewedPartyId = :userId AND t.buyerId = r.reviewerId")
    Page<Review> findReviewsFromBuyers(@Param("userId") Long userId, Pageable pageable);

  
    // (Người viết review phải là Seller trong transaction)
    @EntityGraph(attributePaths = {"transaction"})
    @Query("SELECT r FROM Review r JOIN r.transaction t WHERE r.reviewedPartyId = :userId AND t.sellerId = r.reviewerId")
    Page<Review> findReviewsFromSellers(@Param("userId") Long userId, Pageable pageable);
}