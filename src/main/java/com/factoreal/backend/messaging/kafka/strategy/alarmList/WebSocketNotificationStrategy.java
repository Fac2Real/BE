package com.factoreal.backend.messaging.kafka.strategy.alarmList;

import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class WebSocketNotificationStrategy implements NotificationStrategy {
    // SimpMessagingTemplate은 WebSocketConfig.java에 EnableWebSocketMessageBroker 어노테이션에 의해 빈이 등록됨.
    private final WebSocketSender webSocketSender;

    private static final String userId = "alarm-test";

    @Override
    public void send(AlarmEventDto alarmEventDto) {
        log.info("🌐WebSocket Notification Strategy");
        // /topic/userId로 메세지를 전송 => userId를 구분하여 웹 알람 발송
        // TODO 대시보드 전체에서 보여져야 하는 로직이면 고정 토픽으로 구분없이 보여주는 것도 좋을 듯
        webSocketSender.sendDangerAlarm(alarmEventDto);

    }

    @Override
    public RiskLevel getSupportedLevel() {
        return RiskLevel.INFO;
    }
}
