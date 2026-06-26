package edu.uth.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsGlobalConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowCredentials(true);
        // origin của frontend (thêm origin khác nếu cần)
        cfg.setAllowedOrigins(List.of("http://18.140.200.250:*","http://localhost:9000", "http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:8080", "http://127.0.0.1:8080", "http://localhost:8081", "http://127.0.0.1:8081, "));
        // cho phép header Authorization để gửi Bearer token
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // expose Authorization nếu bạn muốn đọc header trả về
        cfg.setExposedHeaders(List.of("Authorization"));
        // phương thức cho phép
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(src);
    }
}
