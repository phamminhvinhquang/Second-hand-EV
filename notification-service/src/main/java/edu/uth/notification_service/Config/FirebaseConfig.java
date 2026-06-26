// File: edu/uth/notificationservice/Config/FirebaseConfig.java
package edu.uth.notification_service.Config;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions; // (Lưu ý: có thể là javax.annotation.PostConstruct nếu dùng Spring Boot 2)
import com.google.firebase.messaging.FirebaseMessaging;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

    // Lấy đường dẫn file config từ application.properties
    @Value("${app.firebase.config-file}")
    private Resource serviceAccountResource;

    /**
     * Khởi tạo Firebase App khi service khởi động
     */
    @PostConstruct
    public void initializeFirebase() throws IOException {
        InputStream serviceAccountStream = serviceAccountResource.getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
            .build();

        // Khởi tạo App, chỉ một lần duy nhất
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
            System.out.println("Firebase App đã được khởi tạo thành công!");
        }
    }

    /**
     * Cung cấp một Bean FirebaseMessaging để các Service khác có thể @Autowired
     */
    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        return FirebaseMessaging.getInstance(FirebaseApp.getInstance());
    }
}