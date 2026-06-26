package edu.uth.listingservice.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Client sẽ subscribe vào các topic bắt đầu bằng /topic hoặc /user
        config.enableSimpleBroker("/topic", "/user");
        // Client sẽ gửi tin nhắn đến các đích bắt đầu bằng /app
        config.setApplicationDestinationPrefixes("/app");
        // Dùng /user để gửi tin nhắn private cho user
        config.setUserDestinationPrefix("/user"); 
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}