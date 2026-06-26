package edu.uth.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ JWT Filter:
 * - Lấy roles trực tiếp từ token (không cần query DB)
 * - Cho phép các đường dẫn public
 * - Gắn Authentication vào SecurityContext nếu token hợp lệ
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    private static final String[] PUBLIC_PREFIX = {
            "/api/auth", "/public", "/static", "/css/", "/js/", "/images/", "/favicon.ico"
    };

    /** ✅ Xác định đường dẫn public */
    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Cho phép preflight (CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        // Cho phép GET /api/user/{id}
        if ("GET".equalsIgnoreCase(request.getMethod()) && path.matches("^/api/user/\\d+$")) {
            return true;
        }

        for (String prefix : PUBLIC_PREFIX) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // ✅ Nếu public path → bỏ qua kiểm tra JWT
            if (isPublicPath(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = extractToken(request);
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // ✅ Kiểm tra tính hợp lệ token
            if (!jwtUtil.validateToken(token)) {
                log.warn("❌ Invalid JWT for {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            Integer userId = jwtUtil.extractUserId(token);
            Set<String> roles = jwtUtil.extractRoles(token);

            if (userId == null) {
                log.warn("⚠️ Token missing user ID");
                filterChain.doFilter(request, response);
                return;
            }

            // Nếu đã có authentication → bỏ qua
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // ✅ Convert roles → authorities
            Collection<SimpleGrantedAuthority> authorities = roles.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(r -> "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // ✅ Set Authentication vào SecurityContext
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("✅ Authenticated userId={} roles={}", userId, roles);

        } catch (Exception ex) {
            log.error("❌ Error in JwtAuthenticationFilter: {}", ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }

    /** ✅ Trích token từ Header hoặc Cookie */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
