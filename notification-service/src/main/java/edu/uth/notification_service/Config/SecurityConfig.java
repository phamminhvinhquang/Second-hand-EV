// File: edu/uth/notification_service/Config/SecurityConfig.java

package edu.uth.notification_service.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // <--- THÊM IMPORT
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // <--- THÊM ANNOTATION NÀY
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) 
            .cors(Customizer.withDefaults()) 
            .authorizeHttpRequests(auth -> auth
                // [SỬA LẠI] Phải cho phép /ws/**
                .requestMatchers("/ws/**").permitAll() 
                
                // Giữ nguyên rule cho API
                .requestMatchers("/api/**").permitAll() 
                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}