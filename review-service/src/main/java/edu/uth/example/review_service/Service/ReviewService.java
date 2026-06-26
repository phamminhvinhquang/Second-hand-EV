package edu.uth.example.review_service.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import edu.uth.example.review_service.DTO.OrderCompletedEventDTO;
import edu.uth.example.review_service.DTO.ReviewCreatedDTO;
import edu.uth.example.review_service.DTO.ReviewDTO;
import edu.uth.example.review_service.DTO.ReviewRequestDTO;
import edu.uth.example.review_service.DTO.UserReviewStatsDTO;
import edu.uth.example.review_service.Model.Review;
import edu.uth.example.review_service.Model.ReviewableTransaction;
import edu.uth.example.review_service.Repository.ReviewRepository;
import edu.uth.example.review_service.Repository.ReviewableTransactionRepository;

@Service
public class ReviewService {

    @Autowired
    private ReviewableTransactionRepository transactionRepo;

    @Autowired
    private ReviewRepository reviewRepo;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate; 

    @Value("${app.rabbitmq.review-events.exchange}")
    private String reviewExchange;

    /**
     * 1. Tạo giao dịch chờ đánh giá
     */
    @Transactional
    public ReviewableTransaction createReviewableTransaction(OrderCompletedEventDTO dto) {
        if (transactionRepo.findByProductId(dto.getProductId()).isPresent()) {
            return transactionRepo.findByProductId(dto.getProductId()).get();
        }

        Date expiresAt = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));

        ReviewableTransaction tx = new ReviewableTransaction(
                dto.getProductId(),
                dto.getSellerId(),
                dto.getBuyerId(),
                expiresAt,
                dto.getProductName(),
                dto.getPrice());

        ReviewableTransaction savedTx = transactionRepo.save(tx);

        // Sau khi commit: clear cache + gửi WS NEW_TASK cho buyer & seller
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Clear cache
                evictReviewTaskCaches(savedTx.getBuyerId());
                evictReviewTaskCaches(savedTx.getSellerId());

                // Gửi WebSocket: có nhiệm vụ đánh giá mới
                try {
                    String destBuyer = "/topic/user/" + savedTx.getBuyerId() + "/review-tasks/new";
                    String destSeller = "/topic/user/" + savedTx.getSellerId() + "/review-tasks/new";

                    messagingTemplate.convertAndSend(destBuyer, "NEW_TASK");
                    messagingTemplate.convertAndSend(destSeller, "NEW_TASK");

                    System.out.println("WS: sent NEW_TASK to " + destBuyer + " and " + destSeller);
                } catch (Exception e) {
                    System.err.println("WS error when sending NEW_TASK: " + e.getMessage());
                }
            }
        });

        return savedTx;
    }

    /**
     * 2. Người dùng gửi đánh giá mới
     */
    @Transactional
    public Review submitReview(Long currentUserId, ReviewRequestDTO dto) {
        ReviewableTransaction tx = transactionRepo.findByProductId(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Giao dịch không tồn tại hoặc không được phép đánh giá."));

        Long reviewerId = currentUserId;
        Long reviewedPartyId;

        if (currentUserId.equals(tx.getBuyerId())) {
            if (tx.isBuyerReviewed())
                throw new IllegalStateException("Bạn đã đánh giá giao dịch này rồi.");
            reviewedPartyId = tx.getSellerId();
            tx.setBuyerReviewed(true);
        } else if (currentUserId.equals(tx.getSellerId())) {
            if (tx.isSellerReviewed())
                throw new IllegalStateException("Bạn đã đánh giá giao dịch này rồi.");
            reviewedPartyId = tx.getBuyerId();
            tx.setSellerReviewed(true);
        } else {
            throw new SecurityException("Bạn không có quyền đánh giá giao dịch này.");
        }

        String reviewerName = (dto.getReviewerName() != null && !dto.getReviewerName().isEmpty())
                ? dto.getReviewerName()
                : "Người dùng ẩn danh";

        Review review = new Review(tx, reviewerId, reviewedPartyId, dto.getRating(), dto.getComment(), reviewerName);

        Review savedReview = reviewRepo.save(review);
        transactionRepo.save(tx);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 1. Xóa cache thống kê của người BỊ đánh giá (Rating thay đổi)
                evictUserStatsAndPublicPages(reviewedPartyId);

                // 2. Xóa cache task và history của người ĐI đánh giá (Chuyển từ Todo -> Done)
                evictReviewTaskCaches(reviewerId);
                cacheManager.getCache("reviewHistory").clear(); // Xóa toàn bộ history page

                // 3. Gửi event RabbitMQ
                try {
                    ReviewCreatedDTO eventPayload = new ReviewCreatedDTO(savedReview);
                    rabbitTemplate.convertAndSend(reviewExchange, "review.created", eventPayload);
                } catch (Exception e) {
                    System.err.println("❌ Lỗi RabbitMQ: " + e.getMessage());
                }

                // 4. Gửi WS: nhiệm vụ đã hoàn thành cho reviewer (giảm badge)
                try {
                    String destCompleted = "/topic/user/" + reviewerId + "/review-tasks/completed";
                    messagingTemplate.convertAndSend(destCompleted, "COMPLETED_TASK");
                    System.out.println("WS: sent COMPLETED_TASK to " + destCompleted);
                } catch (Exception e) {
                    System.err.println("WS error when sending COMPLETED_TASK: " + e.getMessage());
                }

                // 5. (Tuỳ chọn) Gửi WS cho người được đánh giá: có review mới
                try {
                    String destReviewed = "/topic/user/" + reviewedPartyId + "/review-notifications/new";
                    messagingTemplate.convertAndSend(destReviewed, "NEW_REVIEW");
                } catch (Exception ex) {
                    // Không làm chết luồng chính
                }
            }
        });

        return savedReview;
    }

    /**
     * 3. Chỉnh sửa đánh giá
     */
    @Transactional
    public Review updateReview(Long reviewId, Long currentUserId, ReviewRequestDTO dto) {
        Review existingReview = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá này."));

        if (!existingReview.getReviewerId().equals(currentUserId)) {
            throw new SecurityException("Bạn không có quyền sửa đánh giá này.");
        }

        long daysSinceCreation = ChronoUnit.DAYS
                .between(existingReview.getCreatedAt().toInstant(), Instant.now());
        if (daysSinceCreation > 15)
            throw new IllegalStateException("Đã quá 15 ngày, không thể sửa đánh giá.");
        if (existingReview.getUpdatedAt() != null)
            throw new IllegalStateException("Đánh giá này đã được sửa 1 lần.");

        existingReview.setRating(dto.getRating());
        existingReview.setComment(dto.getComment());
        if (dto.getReviewerName() != null && !dto.getReviewerName().isEmpty()) {
            existingReview.setReviewerName(dto.getReviewerName());
        }
        existingReview.setUpdatedAt(new Date());

        Review updatedReview = reviewRepo.save(existingReview);
        Long reviewedPartyId = updatedReview.getReviewedPartyId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Rating thay đổi -> Xóa thống kê của người bị đánh giá
                evictUserStatsAndPublicPages(reviewedPartyId);

                // Comment thay đổi -> Xóa history của người viết
                cacheManager.getCache("reviewHistory").clear();

                try {
                    ReviewCreatedDTO eventPayload = new ReviewCreatedDTO(updatedReview);
                    rabbitTemplate.convertAndSend(reviewExchange, "review.created", eventPayload);
                } catch (Exception e) {
                    System.err.println("❌ Lỗi RabbitMQ: " + e.getMessage());
                }
            }
        });

        return updatedReview;
    }

    /**
     * 4. Lấy thống kê đánh giá -> CACHE CAO
     */
    @Cacheable(value = "userReviewStats", key = "#userId")
    public UserReviewStatsDTO getReviewStatsForUser(Long userId) {
        List<Object[]> results = reviewRepo.getReviewStatsForUser(userId);
        if (results == null || results.isEmpty()) {
            return new UserReviewStatsDTO(userId, 0.0, 0L);
        }
        Object[] row = results.get(0);
        Double averageRating = (row[0] != null) ? ((Number) row[0]).doubleValue() : 0.0;
        Long totalReviews = (row[1] != null) ? ((Number) row[1]).longValue() : 0L;

        return new UserReviewStatsDTO(userId, averageRating, totalReviews);
    }

    public List<Review> getReviewsForUser(Long userId) {
        return reviewRepo.findByReviewedPartyId(userId);
    }

    /**
     * 5. Lấy danh sách "Việc cần làm" -> CACHE NGẮN HẠN
     */
    @Cacheable(value = "reviewTasks", key = "#userId + '-' + #page + '-' + #size")
    @Transactional(readOnly = true)
    public Page<ReviewableTransaction> getTasksToReview(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "expiresAt"));
        Page<ReviewableTransaction> taskPage = transactionRepo.findTasksToReviewByUserId(userId, pageable);

        taskPage.getContent().forEach(tx -> {
            if (tx.getReviews() != null) {
                tx.setReviews(new ArrayList<>(tx.getReviews()));
            }
        });

        return taskPage;
    }

    /**
     * 6. Lấy danh sách "Lịch sử" -> CACHE TRUNG BÌNH
     */
    @Cacheable(value = "reviewHistory", key = "#userId + '-' + #page + '-' + #size")
    @Transactional(readOnly = true)
    public Page<ReviewableTransaction> getTasksCompleted(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expiresAt"));
        Page<ReviewableTransaction> historyPage = transactionRepo.findTasksCompletedByUserId(userId, pageable);

        historyPage.getContent().forEach(tx -> {
            if (tx.getReviews() != null) {
                tx.setReviews(new ArrayList<>(tx.getReviews()));
            }
        });

        return historyPage;
    }

    /**
     * 7. Lấy danh sách Review VỀ NGƯỜI NÀY (Profile) -> CACHE QUAN TRỌNG
     */
    @Cacheable(value = "userReviewsPage", key = "#userId + '-' + #page + '-' + #size + '-' + #role")
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsAboutUser(Long userId, int page, int size, String role) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage;

        if ("BUYER".equalsIgnoreCase(role)) {
            reviewPage = reviewRepo.findReviewsFromBuyers(userId, pageable);
        } else if ("SELLER".equalsIgnoreCase(role)) {
            reviewPage = reviewRepo.findReviewsFromSellers(userId, pageable);
        } else {
            reviewPage = reviewRepo.findByReviewedPartyId(userId, pageable);
        }
        return reviewPage.map(ReviewDTO::new);
    }

    /**
     * 8. Lấy danh sách Review MÀ NGƯỜI NÀY ĐÃ VIẾT
     */
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewRepo.findByReviewerId(userId, pageable).map(ReviewDTO::new);
    }

    /**
     * 9. Đồng bộ tên người dùng
     */
    @Transactional
    public void updateReviewerName(Long userId, String newName) {
        if (userId == null || newName == null || newName.isEmpty())
            return;

        List<Review> reviewsToUpdate = reviewRepo.findByReviewerId(userId);
        if (reviewsToUpdate.isEmpty())
            return;

        for (Review review : reviewsToUpdate) {
            review.setReviewerName(newName);
        }
        reviewRepo.saveAll(reviewsToUpdate);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheManager.getCache("userReviewsPage").clear();
            }
        });
    }

    /**
     * Đếm số lượng review pending (Badge) -> Cache ngắn
     */
    @Cacheable(value = "reviewTasksCount", key = "#userId")
    public long countPendingReviews(Long userId) {
        return transactionRepo.countTasksToReview(userId);
    }

    // =======================================================
    // HÀM HELPER XÓA CACHE
    // =======================================================

    private void evictReviewTaskCaches(Long userId) {
        if (userId == null)
            return;
        try {
            cacheManager.getCache("reviewTasksCount").evictIfPresent(userId);
            cacheManager.getCache("reviewTasks").clear();
        } catch (Exception e) {
            System.err.println("Lỗi evict cache tasks: " + e.getMessage());
        }
    }

    private void evictUserStatsAndPublicPages(Long userId) {
        if (userId == null)
            return;
        try {
            cacheManager.getCache("userReviewStats").evictIfPresent(userId);
            cacheManager.getCache("userReviewsPage").clear();
        } catch (Exception e) {
            System.err.println("Lỗi evict cache stats: " + e.getMessage());
        }
    }
}
