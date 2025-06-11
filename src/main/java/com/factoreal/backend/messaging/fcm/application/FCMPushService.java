package com.factoreal.backend.messaging.fcm.application;

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

//   TODO 트러블 슈팅 -> 비동기에서 예외처리 방법
    @Async
    public CompletableFuture<String> sendMessage(String token, String title, String body) {
        if (token == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("fcm 토큰 없음"));
        }
        try {
            String message = firebaseMessaging.send(
                Message.builder()
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
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
