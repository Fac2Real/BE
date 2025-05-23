package com.factoreal.backend.domain.notifyLog.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final SimpMessagingTemplate simpMessagingTemplate;
    // 웹소켓 테스트용 사이트
    // 1. 스프링 실행후 아래사이트에서 http://localhost:8080/websocket 으로 연결 (sockjs,stomp 체크)
    // 2. /topic/notify 구독
    // 3. swagger에서 아래 api 호출
    // https://jiangxy.github.io/websocket-debug-tool/
    @PostMapping("/api/notify")
    @Deprecated
    public ResponseEntity<String> notify(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        simpMessagingTemplate.convertAndSend("/topic/notify", message);
        return ResponseEntity.ok("Message sent");
    }
}
