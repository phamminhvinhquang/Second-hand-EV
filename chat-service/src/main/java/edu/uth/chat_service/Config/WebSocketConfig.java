
package edu.uth.chat_service.Config;

import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        if (request instanceof ServletServerHttpRequest) {
                            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
                            String userId = servletRequest.getParameter("userId");
                            
                            // --- THÊM LOG ĐỂ KIỂM TRA ---
                            System.out.println("🔌 [WebSocket] Đang kết nối... UserID param: " + userId);
                            
                            if (userId != null) {
                                attributes.put("userId", Long.valueOf(userId));
                                System.out.println(" [WebSocket] Đã lưu UserID " + userId + " vào Session.");
                            } else {
                                System.err.println(" [WebSocket] Không tìm thấy UserID trong URL!");
                            }
                        }
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                               WebSocketHandler wsHandler, Exception exception) {
                    }
                })
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
}