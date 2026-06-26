package edu.uth.chat_service.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tắt CSRF
            .cors(Customizer.withDefaults()) //  Kích hoạt bean 'corsConfigurationSource' từ WebConfig
            
            // Phân quyền request
            .authorizeHttpRequests(auth -> auth
                // Mở quyền cho các đường dẫn đặc biệt của Chat Service
                .requestMatchers("/ws/**").permitAll()       // Cho phép kết nối WebSocket
                .requestMatchers("/chat-files/**").permitAll()  // Cho phép truy cập file ảnh/video
                
                // Mở quyền cho API (giống file mẫu của bạn)
                .requestMatchers("/api/**").permitAll() 
                .anyRequest().permitAll()
            )
            
            // Cấu hình session là STATELESS (quan trọng cho API/WebSocket)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Tắt các cơ chế login cũ (giống file mẫu)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}