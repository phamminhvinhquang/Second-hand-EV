package com.example.compare_service.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 🔒 Tắt CSRF cho REST API
            .cors(Customizer.withDefaults()) // ✅ Kích hoạt CorsConfig đã định nghĩa
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll() // Cho phép toàn bộ API
                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}

