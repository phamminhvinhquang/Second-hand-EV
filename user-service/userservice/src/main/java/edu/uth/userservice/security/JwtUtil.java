package edu.uth.userservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ JwtUtil — Quản lý tạo và xác thực JWT token
 * Bao gồm: userId, email (subject), roles, thời hạn 24h.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Thời hạn token = 24h
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L;

    /** 🔑 Sinh key bí mật từ chuỗi jwt.secret (phải >= 32 ký tự cho HS256) */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * ✅ Tạo token gồm subject (email), id và danh sách roles
     */
    public String generateToken(String subject, Integer userId, Set<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRATION_MS);
        Key key = getSigningKey();

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userId);
        claims.put("roles", roles == null ? Collections.emptySet() : roles);

        return Jwts.builder()
                .setSubject(subject)
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 🔍 Giải mã Claims từ token (nếu token hợp lệ)
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            // Token hết hạn
            return ex.getClaims();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * ✅ Trích userId từ token
     */
    public Integer extractUserId(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) return null;
        Object idObj = claims.get("id");
        if (idObj == null) return null;
        if (idObj instanceof Number) return ((Number) idObj).intValue();
        try {
            return Integer.parseInt(String.valueOf(idObj));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * ✅ Trích danh sách roles từ token
     */
    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) return Collections.emptySet();

        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof Collection<?>) {
            return ((Collection<?>) rolesObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    /**
     * ✅ Trích subject (email / identifier)
     */
    public String extractSubject(String token) {
        Claims claims = parseClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * ✅ Kiểm tra token hợp lệ (ký + thời hạn)
     */
    public boolean validateToken(String token) {
        Claims claims = parseClaims(token);
        return claims != null && claims.getExpiration().after(new Date());
    }
}
