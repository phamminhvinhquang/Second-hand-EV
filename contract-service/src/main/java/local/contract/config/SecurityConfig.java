package local.contract.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ⚙️ Cấu hình bảo mật cho Contract-Service.
 * - Tắt CSRF, login form, basic auth
 * - Cho phép toàn bộ API contract được truy cập từ frontend
 * - Chuẩn bị sẵn để dễ mở rộng về JWT sau này
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 🚫 Tắt CSRF (REST API không cần form token)
            .csrf(csrf -> csrf.disable())

            // 🌐 Bật CORS (sử dụng CorsConfig của bạn để cho phép origin khác)
            .cors(cors -> {})

            // ✅ Cấu hình quyền truy cập
            .authorizeHttpRequests(auth -> auth
                // Cho phép toàn quyền truy cập các API của Contract-Service
                .requestMatchers(HttpMethod.POST, "/api/contracts/sign").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/contracts/create").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/contracts/user/**").permitAll()
                .requestMatchers("/contracts/**").permitAll()
                // Các API khác (nếu có) cũng được phép truy cập
                .anyRequest().permitAll()
            )

            // 🚫 Tắt form login và basic auth vì không cần cho REST
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
