package edu.uth.chat_service.Listener;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import edu.uth.chat_service.Repository.ChatUserRepository;

@Component
public class WebSocketEventListener {

    @Autowired private ChatUserRepository chatUserRepo;
    @Autowired private CacheManager cacheManager;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headers.getSessionAttributes();
        
        if (sessionAttributes != null) {
            Long userId = (Long) sessionAttributes.get("userId");
            
            if (userId != null) {
                // Cập nhật DB
                chatUserRepo.findById(userId).ifPresent(user -> {
                    user.setOnline(false);
                    user.setLastActiveAt(new Date());
                    chatUserRepo.save(user);
                    
                    // Xóa cache ngay lập tức (vì đây không phải transaction phức tạp)
                    // Để lần sau gọi API status sẽ lấy được trạng thái mới (Offline)
                    cacheManager.getCache("chat_users").evictIfPresent(userId);
                    System.out.println(" [WebSocket] Disconnect & Evicted Cache for User: " + userId);
                });
            }
        }
    }
}