package edu.uth.chat_service.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import edu.uth.chat_service.Model.BlockUser;
import edu.uth.chat_service.Repository.BlockUserRepository;

@Service
public class BlockService {

    @Autowired private BlockUserRepository blockRepo;
    @Autowired private CacheManager cacheManager;

    // READ: Có cache
    @Cacheable(value = "block_status", key = "#blockerId + '-' + #blockedId")
    public boolean isBlocked(Long blockerId, Long blockedId) {
        return blockRepo.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    // WRITE: Chặn + Xóa Cache sau khi commit
    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (!blockRepo.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            blockRepo.save(new BlockUser(blockerId, blockedId));
            evictBlockCache(blockerId, blockedId);
        }
    }

    // WRITE: Bỏ chặn + Xóa Cache sau khi commit
    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        BlockUser b = blockRepo.findByBlockerIdAndBlockedId(blockerId, blockedId);
        if (b != null) {
            blockRepo.delete(b);
            evictBlockCache(blockerId, blockedId);
        }
    }

    private void evictBlockCache(Long blockerId, Long blockedId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheManager.getCache("block_status").evictIfPresent(blockerId + "-" + blockedId);
                // Xóa chiều ngược lại để đảm bảo tính nhất quán nếu cần check 2 chiều
                cacheManager.getCache("block_status").evictIfPresent(blockedId + "-" + blockerId);
            }
        });
    }
}