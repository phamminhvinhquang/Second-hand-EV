
package edu.uth.example.review_service.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; 
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uth.example.review_service.DTO.OrderCompletedEventDTO;
import edu.uth.example.review_service.DTO.ReviewDTO;
import edu.uth.example.review_service.DTO.ReviewRequestDTO;
import edu.uth.example.review_service.DTO.UserReviewStatsDTO;
import edu.uth.example.review_service.Model.Review;
import edu.uth.example.review_service.Model.ReviewableTransaction;
import edu.uth.example.review_service.Service.ReviewService;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

 
    @PostMapping("/admin/create-fake-transaction")
    public ResponseEntity<ReviewableTransaction> createFakeTransaction(
            @RequestBody OrderCompletedEventDTO dto) {
        ReviewableTransaction tx = reviewService.createReviewableTransaction(dto);
        return ResponseEntity.ok(tx);
    }
    
    @PostMapping
    public ResponseEntity<Review> submitReview(
            @RequestHeader("X-User-Id") Long currentUserId, 
            @RequestBody ReviewRequestDTO dto) {
        Review review = reviewService.submitReview(currentUserId, dto);
        return ResponseEntity.ok(review);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Review> updateReview(
            @PathVariable Long id, 
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestBody ReviewRequestDTO dto) {
        Review updatedReview = reviewService.updateReview(id, currentUserId, dto);
        return ResponseEntity.ok(updatedReview);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String role 
    ) { 
        // Truyền role xuống service
        Page<ReviewDTO> reviews = reviewService.getReviewsAboutUser(userId, page, size, role);
        return ResponseEntity.ok(reviews);
    }
    

    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<UserReviewStatsDTO> getReviewStats(@PathVariable Long userId) {
        UserReviewStatsDTO stats = reviewService.getReviewStatsForUser(userId);
        return ResponseEntity.ok(stats);
    }
    
 
    @GetMapping("/tasks/to-review")
    public ResponseEntity<Page<ReviewableTransaction>> getTasksToReview(
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Page<ReviewableTransaction> tasksPage = reviewService.getTasksToReview(currentUserId, page, size);
        return ResponseEntity.ok(tasksPage);
    }

    @GetMapping("/tasks/completed")
    public ResponseEntity<Page<ReviewableTransaction>> getTasksCompleted(
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Page<ReviewableTransaction> tasksPage = reviewService.getTasksCompleted(currentUserId, page, size);
        return ResponseEntity.ok(tasksPage);
    }
    
  
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Page<ReviewDTO> reviews = reviewService.getReviewsByUser(userId, page, size);
        return ResponseEntity.ok(reviews);
    }


@GetMapping("/tasks/count-pending")
    public ResponseEntity<Long> getPendingReviewCount(@RequestHeader("X-User-Id") Long userId) {
        long count = reviewService.countPendingReviews(userId);
        return ResponseEntity.ok(count);
    }
}