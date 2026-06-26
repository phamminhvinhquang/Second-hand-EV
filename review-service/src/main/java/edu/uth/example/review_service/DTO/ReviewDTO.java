
package edu.uth.example.review_service.DTO;

import java.util.Date;

import edu.uth.example.review_service.Model.Review;
import edu.uth.example.review_service.Model.ReviewableTransaction;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class ReviewDTO {

    // --- Thông tin từ Review ---
    private Long id;
    private Long reviewerId;
    private Long reviewedPartyId;
    private String reviewerName;
    private int rating;
    private String comment;
    private Date createdAt;
    private Date updatedAt;

    // --- Thông tin "làm phẳng" từ Transaction ---
    private Long productId;
    private String productName;
    private Long price;

    /**
     * Constructor để chuyển đổi từ Entity sang DTO.
     * Cần @EntityGraph trong Repository để đảm bảo 'review.getTransaction()' không bị null.
     */
    public ReviewDTO(Review review) {
        // Sao chép trường của Review
        this.id = review.getId();
        this.reviewerId = review.getReviewerId();
        this.reviewedPartyId = review.getReviewedPartyId();
        this.reviewerName = review.getReviewerName();
        this.rating = review.getRating();
        this.comment = review.getComment();
        this.createdAt = review.getCreatedAt();
        this.updatedAt = review.getUpdatedAt();

        // Lấy thông tin Transaction
        ReviewableTransaction transaction = review.getTransaction();
        if (transaction != null) {
            this.productId = transaction.getProductId();
            this.productName = transaction.getProductName();
            this.price = transaction.getPrice();
        }
    }
}