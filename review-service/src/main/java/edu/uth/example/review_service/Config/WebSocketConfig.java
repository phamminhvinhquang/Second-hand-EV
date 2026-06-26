package edu.uth.example.review_service.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket sử dụng STOMP (Simple Text Oriented Messaging Protocol)
 */
@Configuration
@EnableWebSocketMessageBroker // Kích hoạt xử lý tin nhắn WebSocket với message broker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Định nghĩa các endpoint mà Client (frontend) sẽ kết nối
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint mà Frontend kết nối đến (WS_URL = "http://localhost:8086/ws" trong JS)
        registry.addEndpoint("/ws")
                // Cho phép kết nối từ mọi nguồn (Cần cấu hình nghiêm ngặt hơn trong môi trường Production)
                .setAllowedOriginPatterns("*") 
                .withSockJS(); // Bật SockJS để đảm bảo tương thích với các trình duyệt cũ
    }

    /**
     * Cấu hình Message Broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Kích hoạt Simple Broker cho các điểm đến bắt đầu bằng "/topic"
        // Đây là nơi Review Service gửi tín hiệu Realtime cho Frontend đăng ký (/topic/user/{userId}/...)
        config.enableSimpleBroker("/topic");
        
        // (Tùy chọn) Đặt prefix cho các endpoint mà Controller sẽ xử lý (nếu có, không cần thiết cho badge)
        // config.setApplicationDestinationPrefixes("/app"); 
    }
}