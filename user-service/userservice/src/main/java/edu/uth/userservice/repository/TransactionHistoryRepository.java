package edu.uth.userservice.repository;

import edu.uth.userservice.model.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
    
    // ⭐️ Hàm mấu chốt: Lấy lịch sử theo userId, sắp xếp mới nhất lên đầu
    List<TransactionHistory> findByUserIdOrderByCreatedAtDesc(Integer userId);
}