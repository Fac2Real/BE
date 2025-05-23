package com.factoreal.backend.messaging.sender;

import com.factoreal.backend.messaging.common.dto.SystemLogDto;
import com.factoreal.backend.messaging.common.dto.ZoneDangerDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketSender { // 실제로 프론트에 메시지를 전송하는 클래스

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * zone 별로 위험도 메시지를 WebSocket으로 전송
     */
    public void sendDangerLevel(String zoneId, String sensorType, int level) {
        ZoneDangerDto dangerDto = new ZoneDangerDto(zoneId, sensorType, level);
        messagingTemplate.convertAndSend("/topic/zone", dangerDto);
    }

    /**
     * Todo : 시스템 로그를 WebSocket으로 전송 -> restAPI 변경으로 삭제 예정
     */
    public void sendSystemLog(SystemLogDto logDto) {
        messagingTemplate.convertAndSend("/topic/system-log", logDto);
    }

    /**
     * 알람 이벤트를 WebSocket으로 전송
     */
    public void sendDangerAlarm(AlarmEventDto alarmEventDto) {
        messagingTemplate.convertAndSend("/topic/alarm", alarmEventDto);
    }

    /**
     * 읽지 않은 알람수 전송
     */
    public void sendUnreadCount(long count){
        messagingTemplate.convertAndSend("/topic/unread-count", count);
    }
}
