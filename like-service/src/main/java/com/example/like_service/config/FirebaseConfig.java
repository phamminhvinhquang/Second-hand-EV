package com.example.like_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.service.account.path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() {
        if (serviceAccountPath == null || serviceAccountPath.trim().isEmpty()) {
            log.info("firebase.service.account.path not set — skipping Firebase initialization (dev mode)");
            return;
        }

        try {
            InputStream serviceAccount;
            if (serviceAccountPath.startsWith("classpath:")) {
                String cp = serviceAccountPath.substring("classpath:".length());
                serviceAccount = this.getClass().getClassLoader().getResourceAsStream(cp);
                if (serviceAccount == null) {
                    log.warn("Firebase service account resource not found on classpath: {}", cp);
                    return;
                }
            } else {
                File f = new File(serviceAccountPath);
                if (!f.exists() || !f.canRead()) {
                    log.warn("Firebase service account file not found or not readable: {}", serviceAccountPath);
                    return;
                }
                serviceAccount = new FileInputStream(f);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            } else {
                log.info("Firebase already initialized");
            }
        } catch (Exception ex) {
            log.error("Cannot initialize Firebase (notifications will be disabled): {}", ex.getMessage(), ex);
        }
    }
}
