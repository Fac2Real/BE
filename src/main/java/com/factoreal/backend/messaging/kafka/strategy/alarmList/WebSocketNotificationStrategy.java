package com.factoreal.backend.messaging.kafka.strategy.alarmList;

import com.factoreal.backend.domain.notifyLog.dto.TriggerType;
import com.factoreal.backend.domain.notifyLog.application.NotifyLogService;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
@Slf4j
public class WebSocketNotificationStrategy implements NotificationStrategy {
    // SimpMessagingTemplate은 WebSocketConfig.java에 EnableWebSocketMessageBroker 어노테이션에 의해 빈이 등록됨.
    private final WebSocketSender webSocketSender;
    private final NotifyLogService notifyLogService;
    private static final String userId = "alarm-test";

    @Override
    public void send(AlarmEventDto alarmEventDto) {
        log.info("🌐WebSocket Notification Strategy");
        // /topic/userId로 메세지를 전송 => userId를 구분하여 웹 알람 발송
        // 대시보드 전체에서 보여져야 하는 로직이면 고정 토픽으로 구분없이 보여주는 것도 좋을 듯 -> 고정 토픽을 사용중
        try {
            webSocketSender.sendDangerAlarm(alarmEventDto);
            notifyLogService.saveNotifyLogFromWebsocket(
                    "/topic/alarm",
                    Boolean.TRUE,
                    TriggerType.AUTOMATIC,
                    LocalDateTime.parse(alarmEventDto.getTime()),
                    alarmEventDto.getEventId()
            );
        } catch (Exception e) {
            notifyLogService.saveNotifyLogFromWebsocket(
                    "/topic/alarm",
                    Boolean.FALSE,
                    TriggerType.AUTOMATIC,
                    LocalDateTime.parse(alarmEventDto.getTime()),
                    alarmEventDto.getEventId()
            );
        }
    }

    @Override
    public RiskLevel getSupportedLevel() {
        return RiskLevel.INFO;
    }
}
