package edu.uth.chat_service.Listener;

import java.util.Date;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import edu.uth.chat_service.DTO.UserEventDTO;
import edu.uth.chat_service.Model.ChatUser;
import edu.uth.chat_service.Repository.ChatUserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserSyncListener {

    @Autowired private ChatUserRepository chatUserRepo;
    @Autowired private CacheManager cacheManager;

    @RabbitListener(queues = "${app.rabbitmq.user.queue}")
    @Transactional 
    public void syncUser(UserEventDTO event) {
        log.info("📥 [RabbitMQ] Nhận User update: ID={}, Name={}", event.getId(), event.getName());
        
        if (event.getId() == null) return;

        ChatUser user = new ChatUser(
            event.getId(),
            event.getName(),
            event.getEmail(),
            event.getPhone(),
            false,
            new Date()
        );
        
        chatUserRepo.save(user);
        
        // Xóa cache user info sau khi commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheManager.getCache("chat_users").evictIfPresent(event.getId());
                log.info(" Evicted user cache for ID: {}", event.getId());
            }
        });
    }
}