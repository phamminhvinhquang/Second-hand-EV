
package edu.uth.chat_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uth.chat_service.Model.BlockUser;

@Repository
public interface BlockUserRepository extends JpaRepository<BlockUser, Long> {
    // Kiểm tra xem A có chặn B không
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    
    // Tìm để xóa (Bỏ chặn)
    BlockUser findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}