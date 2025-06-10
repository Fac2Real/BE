package com.factoreal.backend.messaging.fcm.application;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FCMPushServiceTest {

    @InjectMocks
    private FCMPushService fcmPushService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Nested
    @DisplayName("sendMessage 메서드 테스트")
    class SendMessageTest {

        @Test
        @DisplayName("✅ 메시지 전송 성공")
        void sendMessageSuccess() throws Exception {
            // given
            String token = "valid-token";
            String title = "title";
            String body = "body";

            when(firebaseMessaging.send(any(Message.class)))
                .thenReturn("message-id-123");

            // when
            CompletableFuture<String> future = fcmPushService.sendMessage(token, title, body);

            // then
            assertEquals("message-id-123", future.get()); // CompletableFuture.get()은 checked Exception 처리 필요
            verify(firebaseMessaging, times(1)).send(any(Message.class));
        }

        @Test
        @DisplayName("❌ FirebaseMessagingException 발생 시 실패")
        void sendMessageFailFirebaseException() throws Exception {
            // given
            String token = "invalid-token";
            String title = "title";
            String body = "body";

            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            when(exception.getMessage()).thenReturn("잘못된 토큰");

            when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(exception);

            // when
            CompletableFuture<String> future = fcmPushService.sendMessage(token, title, body);

            // then
            ExecutionException thrown = assertThrows(ExecutionException.class, future::get);
            assertTrue(thrown.getCause() instanceof FirebaseMessagingException);
            assertEquals("잘못된 토큰", thrown.getCause().getMessage());
        }


        @Test
        @DisplayName("❌ 기타 예외 발생 시 실패")
        void sendMessageFailGenericException() throws Exception {
            // given
            String token = "valid-token";
            String title = "title";
            String body = "body";

            when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(new RuntimeException("Unknown failure"));

            // when
            CompletableFuture<String> future = fcmPushService.sendMessage(token, title, body);

            // then
            ExecutionException thrown = assertThrows(ExecutionException.class, future::get);
            assertTrue(thrown.getCause() instanceof RuntimeException);
            assertEquals("Unknown failure", thrown.getCause().getMessage());
        }
    }
}
