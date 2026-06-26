// File: edu/uth/example/review_service/DTO/ReviewCreatedDTO.java
package edu.uth.example.review_service.DTO;

import edu.uth.example.review_service.Model.Review;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReviewCreatedDTO {
    private Long reviewedPartyId; // Người nhận thông báo
    private Long reviewerId;      // Người đánh giá
    private String reviewerName;  // Tên người đánh giá
    private int rating;           // Số sao
    private String comment;       // Nội dung (nếu cần hiển thị ngắn gọn)

    public ReviewCreatedDTO(Review review) {
        this.reviewedPartyId = review.getReviewedPartyId();
        this.reviewerId = review.getReviewerId();
        // Lấy trực tiếp từ review entity
        this.reviewerName = review.getReviewerName(); 
        this.rating = review.getRating();
        this.comment = review.getComment();
    }
}