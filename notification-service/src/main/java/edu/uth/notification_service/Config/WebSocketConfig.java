// File: edu/uth/notificationservice/Config/WebSocketConfig.java
package edu.uth.notification_service.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Kích hoạt WebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Định nghĩa các tiền tố "chủ đề" (topic) mà client sẽ lắng nghe
        config.enableSimpleBroker("/topic", "/user");
        
        // Định nghĩa tiền tố cho các kênh "cá nhân" (gửi cho 1 user)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Định nghĩa endpoint kết nối WebSocket
        // JavaScript sẽ kết nối tới (ví dụ: ws://localhost:8083/ws)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Cho phép mọi nguồn (thay đổi khi deploy)
                .withSockJS(); // Bật SockJS để dự phòng
    }
}