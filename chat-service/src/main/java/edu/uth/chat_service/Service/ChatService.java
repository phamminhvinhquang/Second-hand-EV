package edu.uth.chat_service.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import edu.uth.chat_service.Model.ChatMessage;
import edu.uth.chat_service.Repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatService {
    @Autowired private ChatMessageRepository msgRepo;
    @Autowired private CacheManager cacheManager;

    // 1. READ: Cache Lịch sử chat
    // Key: Sắp xếp ID để A->B hay B->A đều ra cùng 1 key
    @Cacheable(value = "chat_history", key = "#senderId < #recipientId ? #senderId + '_' + #recipientId : #recipientId + '_' + #senderId")
    public List<ChatMessage> getHistory(Long senderId, Long recipientId) {
        String chatId = getChatId(senderId, recipientId);
        return msgRepo.findHistory(chatId, senderId); 
    }

    // 2. READ: Cache Sidebar (Chuyển từ Controller xuống đây để Cache)
    @Cacheable(value = "chat_sidebar", key = "#userId")
    public List<ChatMessage> getRecentConversations(Long userId) {
        return msgRepo.findRecentConversations(userId);
    }

    // 3. WRITE: Lưu tin nhắn + Xóa Cache an toàn
    @Transactional
    public ChatMessage save(ChatMessage msg) {
        String chatId = getChatId(msg.getSenderId(), msg.getRecipientId());
        msg.setChatId(chatId);
        
        ChatMessage savedMsg = msgRepo.save(msg);

        // Logic xóa cache sau khi Commit DB thành công
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Xóa cache lịch sử của cặp đôi này để hiện tin mới
                cacheManager.getCache("chat_history").evictIfPresent(chatId);

                // Xóa cache danh sách chat (Sidebar) của cả 2 người
                cacheManager.getCache("chat_sidebar").evictIfPresent(msg.getSenderId());
                cacheManager.getCache("chat_sidebar").evictIfPresent(msg.getRecipientId());

                log.info("✅ Evicted cache for ChatID: {}", chatId);
            }
        });

        return savedMsg;
    }

    // 4. DELETE: Xóa cuộc hội thoại
    @Transactional
    public void deleteConversation(Long userId, Long partnerId) {
        String chatId = getChatId(userId, partnerId);
        msgRepo.hideConversationForSender(chatId, userId);
        msgRepo.hideConversationForRecipient(chatId, userId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Xóa history để user không thấy tin cũ
                cacheManager.getCache("chat_history").evictIfPresent(chatId);
                // Xóa sidebar của người xóa để cập nhật lại list
                cacheManager.getCache("chat_sidebar").evictIfPresent(userId);
            }
        });
    }

    private String getChatId(Long senderId, Long recipientId) {
        long min = Math.min(senderId, recipientId);
        long max = Math.max(senderId, recipientId);
        return min + "_" + max;
    }
}