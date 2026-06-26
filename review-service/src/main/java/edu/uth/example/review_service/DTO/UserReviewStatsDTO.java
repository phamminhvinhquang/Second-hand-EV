package edu.uth.example.review_service.DTO;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor           
@AllArgsConstructor
public class UserReviewStatsDTO implements Serializable {
    private Long userId;
    private Double averageRating;
    private Long totalReviews;
}
