package edu.uth.userservice.config;

import edu.uth.userservice.security.JwtAuthenticationFilter;
import edu.uth.userservice.security.oauth2.CustomOAuth2UserService;
import edu.uth.userservice.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired 
    private CustomOAuth2UserService customOAuth2UserService;
    @Autowired 
    private OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // ✅ KÍCH HOẠT OAUTH2
            .oauth2Login(oauth2 ->
                oauth2.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                      .successHandler(oauth2SuccessHandler) 
                      
                      
                      
            )
            // ⬆️ HẾT PHẦN THÊM MỚI ⬆️
            
            // ✅ KHỐI QUYỀN TRUY CẬP
            .authorizeHttpRequests(auth -> {
                // 1️⃣ Luôn cho phép preflight (CORS)
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                // 2️⃣ Cho phép tài nguyên tĩnh public
                auth.requestMatchers("/", "/index.html", "/login.html", "/register.html",
                        "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll();

                // 3️⃣ Auth endpoints (login/register) VÀ OAUTH2
                // (Bao gồm các fix cho JwtAuthenticationFilter)
                auth.requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**", "/login").permitAll();

                // 4️⃣ Cho phép service khác gọi sang (nội bộ)
                auth.requestMatchers(
                        "/api/user/*/roles",
                        "/api/user/*/info",
                        "/actuator/**"
                ).permitAll();

                // 5️⃣ Cho phép GET user info công khai
                auth.requestMatchers(HttpMethod.GET, "/api/user/{id:\\d+}").permitAll();

                // 6️⃣ Khu vực Admin
                auth.requestMatchers("/api/admin/**", "/admin.html", "/admin-panel/**").hasRole("ADMIN");

                // 7️⃣ Khu vực Staff
                auth.requestMatchers("/api/staff/**", "/staff/**").hasAnyRole("STAFF", "ADMIN");

                // 8️⃣ Các endpoint user cần đăng nhập
                auth.requestMatchers("/api/user/**", "/profile.html", "/liked.html", "/compare.html")
                        .authenticated();

                // 9️⃣ Mặc định chặn tất cả còn lại
                auth.anyRequest().authenticated();
            });
            
        // ✅ Thêm JWT filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}