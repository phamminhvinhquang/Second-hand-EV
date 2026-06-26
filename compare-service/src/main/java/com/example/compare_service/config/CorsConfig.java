package com.example.compare_service.config;


import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ Cho phép tất cả localhost & 127.0.0.1 (mọi port)
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        // ✅ Các phương thức được phép
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // ✅ Cho phép mọi header (Content-Type, Authorization, v.v.)
        config.setAllowedHeaders(List.of("*"));

        // ✅ Cho phép cookie hoặc token (nếu có)
        config.setAllowCredentials(true);

        // ✅ Giảm preflight request bằng cách cache trong 1 giờ
        config.setMaxAge(3600L);

        // ✅ Đăng ký áp dụng cho toàn bộ route
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}