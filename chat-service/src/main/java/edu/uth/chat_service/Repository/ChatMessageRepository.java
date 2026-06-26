package edu.uth.chat_service.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.uth.chat_service.Model.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    //  MỚI: Tìm tin nhắn chưa đọc gửi đến user
    @Query("SELECT m FROM ChatMessage m WHERE m.recipientId = :userId AND m.senderId = :partnerId AND m.isRead = false")
    List<ChatMessage> findUnreadMessages(@Param("userId") Long userId, @Param("partnerId") Long partnerId);

    // Lấy lịch sử (trừ tin đã xóa)
    @Query("SELECT m FROM ChatMessage m WHERE m.chatId = :chatId " +
           "AND ((m.senderId = :userId AND m.deletedBySender = false) " +
           "OR (m.recipientId = :userId AND m.deletedByRecipient = false)) " +
           "ORDER BY m.timestamp ASC")
    List<ChatMessage> findHistory(String chatId, Long userId);

    // Xóa phía người gửi
    @Modifying @Transactional
    @Query("UPDATE ChatMessage m SET m.deletedBySender = true WHERE m.chatId = :chatId AND m.senderId = :userId")
    void hideConversationForSender(String chatId, Long userId);

    // Xóa phía người nhận
    @Modifying @Transactional
    @Query("UPDATE ChatMessage m SET m.deletedByRecipient = true WHERE m.chatId = :chatId AND m.recipientId = :userId")
    void hideConversationForRecipient(String chatId, Long userId);
    
    // Lấy danh sách chat sidebar
    @Query(value = """
        SELECT m.* FROM chat_messages m
        INNER JOIN (
            SELECT 
                CASE WHEN sender_id = :userId THEN recipient_id ELSE sender_id END AS partner_id,
                MAX(timestamp) as max_time
            FROM chat_messages
            WHERE (sender_id = :userId AND deleted_by_sender = false) 
               OR (recipient_id = :userId AND deleted_by_recipient = false)
            GROUP BY partner_id
        ) latest ON (
            (m.sender_id = :userId AND m.recipient_id = latest.partner_id) 
            OR (m.recipient_id = :userId AND m.sender_id = latest.partner_id)
        ) AND m.timestamp = latest.max_time
        ORDER BY m.timestamp DESC
    """, nativeQuery = true)
    List<ChatMessage> findRecentConversations(@Param("userId") Long userId);

    // Đếm tổng số tin nhắn chưa đọc của user này
    Long countByRecipientIdAndIsReadFalse(Long recipientId);
}