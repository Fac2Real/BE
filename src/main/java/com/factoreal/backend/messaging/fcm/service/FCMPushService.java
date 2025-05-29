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

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMPushService {
    private final FirebaseMessaging firebaseMessaging;

    @Async
    public void sendMessage(String token, String title, String body) {
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
        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패 (FirebaseMessagingException): {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("FCM 전송 실패 (기타): {}", e.getMessage(), e);
        }
    }
}
