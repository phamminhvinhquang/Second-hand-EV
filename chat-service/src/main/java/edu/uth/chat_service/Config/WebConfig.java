package edu.uth.chat_service.Config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Bean này cung cấp cấu hình CORS (giống hệt file CorsConfig mẫu của bạn).
     * Spring Security sẽ tự động tìm và sử dụng bean này.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Cho phép tất cả localhost & 127.0.0.1 (mọi port)
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        // Các phương thức được phép
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Cho phép mọi header
        config.setAllowedHeaders(List.of("*"));

        // Cho phép cookie/token
        config.setAllowCredentials(true);

        // Cache preflight request
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Cấu hình này phục vụ file tĩnh (ảnh, video) từ thư mục /uploads
     * 
     */
  @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // THAY ĐỔI: Đổi đường dẫn web thành "/chat-files/**"
        // Đường dẫn file system "file:uploads/" vẫn giữ nguyên
        // vì nó trỏ đến thư mục /app/uploads bên trong container
        registry.addResourceHandler("/chat-files/**") 
                .addResourceLocations("file:uploads/");
    }
}