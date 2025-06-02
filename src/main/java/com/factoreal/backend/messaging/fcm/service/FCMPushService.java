package com.factoreal.backend.messaging.fcm.service;

import com.factoreal.backend.messaging.config.FirebaseConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMPushService {
    private final FirebaseMessaging firebaseMessaging;

    @Async
    public CompletableFuture<String> sendMessage(String token, String title, String body) throws FirebaseMessagingException {
        try {
            String message = firebaseMessaging.send(
                Message.builder()
                    .setNotification(
                        Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build()
                    )
                    .setToken(token)
                    .build()
            );
            log.info("FCM 전송 성공: {}", message);
            return CompletableFuture.completedFuture(message);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패 (FirebaseMessagingException): {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            log.error("FCM 전송 실패 (기타): {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}
