package edu.uth.chat_service.Controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uth.chat_service.DTO.NewMessageEventDTO;
import edu.uth.chat_service.Model.ChatMessage;
import edu.uth.chat_service.Model.ChatUser;
import edu.uth.chat_service.Repository.ChatMessageRepository;
import edu.uth.chat_service.Repository.ChatUserRepository;
import edu.uth.chat_service.Service.BlockService;
import edu.uth.chat_service.Service.ChatService;
import edu.uth.chat_service.Service.FileStorageService;

@RestController
public class ChatController {

    @Autowired private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired private ChatService chatService;
    @Autowired private BlockService blockService; // Dùng service mới
    @Autowired private ChatUserRepository chatUserRepo;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private CacheManager cacheManager; // Inject để xóa cache khi recall

    @Value("${app.rabbitmq.chat.routing-key}") private String chatRoutingKey;

    @MessageMapping("/private-message")
    public void receiveMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        
        if(headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("userId", message.getSenderId());
        }

        // [SỬA] Dùng BlockService có cache
        if (blockService.isBlocked(message.getRecipientId(), message.getSenderId())) {
            ChatMessage errorMsg = new ChatMessage();
            errorMsg.setContent("BLOCKED_BY_USER"); 
            errorMsg.setSenderId(0L); 
            simpMessagingTemplate.convertAndSend("/queue/messages/" + message.getSenderId(), errorMsg);
            return;
        }

        Optional<ChatUser> userOpt = chatUserRepo.findById(message.getSenderId());
        String realName = userOpt.map(ChatUser::getFullName).orElse("User " + message.getSenderId());
        message.setSenderName(realName);

        // [SỬA] chatService.save() đã lo việc xóa cache Sidebar/History
        ChatMessage saved = chatService.save(message);

        simpMessagingTemplate.convertAndSend("/queue/messages/" + saved.getRecipientId(), saved);
        simpMessagingTemplate.convertAndSend("/queue/messages/" + saved.getSenderId(), saved);

        NewMessageEventDTO event = new NewMessageEventDTO(saved.getSenderId(), saved.getSenderName(), saved.getRecipientId(), saved.getContent(), saved.getProductId());
        try { 
            rabbitTemplate.convertAndSend("listing.events.exchange", chatRoutingKey, event); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @DeleteMapping("/api/chat/recall/{messageId}")
    @Transactional // [SỬA] Thêm Transactional để dùng registerSynchronization
    public ResponseEntity<?> recallMessage(@PathVariable Long messageId, @RequestParam Long userId) {
        Optional<ChatMessage> msgOpt = chatMessageRepository.findById(messageId);
        if (msgOpt.isEmpty()) return ResponseEntity.notFound().build();

        ChatMessage msg = msgOpt.get();
        if (!msg.getSenderId().equals(userId)) return ResponseEntity.status(403).body("Không có quyền.");

        Instant msgTime = msg.getTimestamp().toInstant();
        if (msgTime.plus(24, ChronoUnit.HOURS).isBefore(Instant.now())) {
            return ResponseEntity.badRequest().body("Quá 24h.");
        }

        if ("IMAGE".equals(msg.getMsgType()) || "VIDEO".equals(msg.getMsgType())) {
            fileStorageService.deleteFile(msg.getContent());
        }

        msg.setRecalled(true);
        chatMessageRepository.save(msg);

        // [SỬA] Logic xóa cache sau khi commit thu hồi
        String chatId = msg.getChatId();
        Long sender = msg.getSenderId();
        Long recipient = msg.getRecipientId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Clear history để tin nhắn hiển thị là "Đã thu hồi"
                cacheManager.getCache("chat_history").evictIfPresent(chatId);
                // Clear sidebar vì tin nhắn cuối cùng thay đổi nội dung
                cacheManager.getCache("chat_sidebar").evictIfPresent(sender);
                cacheManager.getCache("chat_sidebar").evictIfPresent(recipient);
            }
        });

        Map<String, Object> recallEvent = new HashMap<>();
        recallEvent.put("type", "RECALL");
        recallEvent.put("messageId", messageId);

        simpMessagingTemplate.convertAndSend("/queue/messages/" + msg.getRecipientId(), recallEvent);
        simpMessagingTemplate.convertAndSend("/queue/messages/" + msg.getSenderId(), recallEvent);

        return ResponseEntity.ok("Recalled and File Deleted");
    }

    @GetMapping("/api/chat/history/{user1}/{user2}")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable Long user1, @PathVariable Long user2) {
        // [SỬA] Gọi qua Service để tận dụng @Cacheable
        List<ChatMessage> history = chatService.getHistory(user1, user2);
        
        for (ChatMessage msg : history) {
            if (msg.isRecalled()) {
                msg.setContent("Tin nhắn đã bị thu hồi");
                msg.setMsgType("TEXT"); 
            }
        }
        return ResponseEntity.ok(history);
    }

    @GetMapping("/api/chat/conversations/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getConversations(@PathVariable Long userId) {
        // [SỬA] Gọi qua Service để tận dụng @Cacheable("chat_sidebar")
        List<ChatMessage> messages = chatService.getRecentConversations(userId);
        
        List<Map<String, Object>> conversations = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, Object> map = new HashMap<>();
            boolean isMeSender = msg.getSenderId().equals(userId);
            Long partnerId = isMeSender ? msg.getRecipientId() : msg.getSenderId();
            
            String partnerName;
            if (!isMeSender) {
                partnerName = msg.getSenderName(); 
            } else {
                Optional<ChatUser> pUser = chatUserRepo.findById(partnerId);
                partnerName = pUser.map(ChatUser::getFullName).orElse("User " + partnerId);
            }

            map.put("partnerId", partnerId);
            map.put("partnerName", partnerName != null ? partnerName : "User " + partnerId);
            
            if (msg.isRecalled()) {
                map.put("lastMessage", "Tin nhắn đã bị thu hồi");
            } else {
                map.put("lastMessage", msg.getContent());
            }
            map.put("msgType", msg.getMsgType()); 
            map.put("timestamp", msg.getTimestamp());
            boolean isUnread = !isMeSender && !msg.isRead();
            map.put("unread", isUnread); 

            conversations.add(map);
        }
        return ResponseEntity.ok(conversations);
    }

    // [SỬA] Các API Block/Unblock gọi qua BlockService
    @PostMapping("/api/chat/block")
    public ResponseEntity<?> block(@RequestParam Long blockerId, @RequestParam Long blockedId) {
        blockService.blockUser(blockerId, blockedId);
        return ResponseEntity.ok("Blocked");
    }

    @PostMapping("/api/chat/unblock")
    public ResponseEntity<?> unblock(@RequestParam Long blockerId, @RequestParam Long blockedId) {
        blockService.unblockUser(blockerId, blockedId);
        return ResponseEntity.ok("Unblocked");
    }

    @GetMapping("/api/chat/check-block")
    public ResponseEntity<?> checkBlock(@RequestParam Long user1, @RequestParam Long user2) {
        Map<String, Boolean> r = new HashMap<>();
        // Dùng service để tận dụng cache
        r.put("isBlockedByMe", blockService.isBlocked(user1, user2));
        r.put("isBlockedByOther", blockService.isBlocked(user2, user1));
        return ResponseEntity.ok(r);
    }

    // Các API khác giữ nguyên
    @PostMapping("/api/chat/connect")
    public ResponseEntity<?> connectUser(@RequestParam Long userId) {
        chatUserRepo.findById(userId).ifPresent(u -> { u.setOnline(true); chatUserRepo.save(u); });
        return ResponseEntity.ok("Connected");
    }

    @DeleteMapping("/api/chat/conversation")
    public ResponseEntity<?> delConv(@RequestParam Long userId, @RequestParam Long partnerId) {
        chatService.deleteConversation(userId, partnerId);
        return ResponseEntity.ok("Deleted");
    }

    @GetMapping("/api/chat/user-status/{id}")
    public ResponseEntity<?> status(@PathVariable Long id) { return ResponseEntity.of(chatUserRepo.findById(id)); }
    
@PostMapping("/api/chat/mark-read")
    public ResponseEntity<?> read(@RequestParam Long userId, @RequestParam Long partnerId) {
        List<ChatMessage> l = chatMessageRepository.findUnreadMessages(userId, partnerId);
        if (!l.isEmpty()) {
            for (ChatMessage m : l) {
                m.setRead(true);
                chatMessageRepository.save(m);
            }
            
            Map<String, Object> r = new HashMap<>();
            r.put("type", "READ_RECEIPT");
            r.put("readerId", userId);
            r.put("partnerId", partnerId);

            // 1. Báo cho người gửi biết (để hiện "Đã xem" bên khung chat của họ) - GIỮ NGUYÊN
            simpMessagingTemplate.convertAndSend("/queue/messages/" + partnerId, r);
            
            // 2.  Báo cho chính người đọc (để Badge của họ tự giảm số xuống)
            // Logic: Khi nhận được tin có type="READ_RECEIPT", file chat-badge.js sẽ tự fetch lại số lượng.
            simpMessagingTemplate.convertAndSend("/queue/messages/" + userId, r);
        }
        return ResponseEntity.ok("Read");
    }

    @GetMapping("/api/chat/unread-count/{userId}")
    public ResponseEntity<Long> getUnreadMsgCount(@PathVariable Long userId) {
        long count = chatMessageRepository.countByRecipientIdAndIsReadFalse(userId);
        return ResponseEntity.ok(count);
    }
}