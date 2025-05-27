package com.factoreal.backend.domain.notifyLog.api;

import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notify")
@Tag(name = "알람 직접 발생 API", description = "작업자 호출(FCM), 문자(aws 리전으로 SMS 지원안되서 Slack으로 대체)을 위한 API")
@RequiredArgsConstructor
public class NotificationController {
    private final WebSocketSender webSocketSender;
    // 웹소켓 테스트용 사이트
    // 1. 스프링 실행후 아래사이트에서 http://localhost:8080/websocket 으로 연결 (sockjs,stomp 체크)
    // 2. /topic/notify 구독
    // 3. swagger에서 아래 api 호출
    // https://jiangxy.github.io/websocket-debug-tool/
    @PostMapping("/test")
    @Operation(summary = "테스트용", description = "화면이 없을 때 사용한 웹소켓 테스트용 api - 일괄 삭제 예정")
    public ResponseEntity<String> notify(@RequestBody AlarmEventDto alarmEventDto) {
        alarmEventDto.setTime(LocalDateTime.now().toString());
        webSocketSender.sendDangerAlarm(alarmEventDto);
        return ResponseEntity.ok("Message sent");
    }

    @PostMapping("/send/fcm")
    @Operation(summary = "FCM 전송", description = "WokerId에 해당되는 FCm토큰을 조회하여 전송")
    public ResponseEntity<String> sendFcm() {
        return ResponseEntity.ok("FCM 전송");
    }
}
