package com.factoreal.backend.messaging.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


// 아래 블로그를 참고하여 구현
// https://velog.io/@joeun-01/Spring-Boot-FCM%EC%9C%BC%EB%A1%9C-%ED%91%B8%EC%8B%9C-%EC%95%8C%EB%A6%BC-%EA%B5%AC%ED%98%84%ED%95%98%EA%B8%B0
@Configuration
public class FirebaseConfig {

    @Value("${firebase.json-base64}")
    private String firebaseServiceAccountJsonBase64;

    // FCM 푸시 알림 객체를 싱글톤으로 사용
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        try {
            // Base64 디코딩
            byte[] decoded = Base64.getDecoder().decode(firebaseServiceAccountJsonBase64.getBytes(StandardCharsets.UTF_8));

            // InputStream 으로 래핑
            ByteArrayInputStream serviceAccount = new ByteArrayInputStream(decoded);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            return FirebaseMessaging.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
