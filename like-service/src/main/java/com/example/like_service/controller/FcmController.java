package com.example.like_service.controller;

import com.example.like_service.model.FcmToken;
import com.example.like_service.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmTokenRepository tokenRepository;

    // body: { "token": "xxx", "platform":"web" }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                      @RequestBody FcmToken body) {
        if (userId == null) {
            return ResponseEntity.status(401).body("X-User-Id header required");
        }
        if (body == null || body.getToken() == null) return ResponseEntity.badRequest().body("token required");
        Optional<com.example.like_service.model.FcmToken> opt = tokenRepository.findByToken(body.getToken());
        com.example.like_service.model.FcmToken token;
        if (opt.isPresent()) {
            token = opt.get();
            token.setUserId(userId);
            token.setPlatform(body.getPlatform());
        } else {
            token = com.example.like_service.model.FcmToken.builder()
                    .userId(userId)
                    .token(body.getToken())
                    .platform(body.getPlatform())
                    .build();
        }
        tokenRepository.save(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unregister")
    public ResponseEntity<?> unregister(@RequestBody FcmToken body) {
        if (body == null || body.getToken() == null) return ResponseEntity.badRequest().body("token required");
        tokenRepository.deleteByToken(body.getToken());
        return ResponseEntity.ok().build();
    }
}
