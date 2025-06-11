package com.factoreal.backend.messaging.sender;

import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.factoreal.backend.messaging.common.dto.SystemLogDto;
import com.factoreal.backend.messaging.common.dto.ZoneDangerDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventResponse;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

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
    public void sendDangerAlarm(AlarmEventResponse alarmEventResponse) {
        messagingTemplate.convertAndSend("/topic/alarm", alarmEventResponse);
    }

    /**
     * 읽지 않은 알람수 전송
     */
    public void sendUnreadCount(long count) {
        messagingTemplate.convertAndSend("/topic/unread-count", count);
    }

    /**
     * 제어 상태를 WebSocket으로 전송하고 FE에서 발송 여부를 확인할 수 있도록 함
     */
    public void sendControlStatus(ControlLog controlLog, Map<String, Boolean> deliveryStatus) {
        Map<String, Object> status = new HashMap<>();
        status.put("controlId", controlLog.getId());
        status.put("controlType", controlLog.getControlType());
        status.put("controlValue", controlLog.getControlVal());
        status.put("controlStatus", controlLog.getControlStat());
        status.put("executedAt", controlLog.getExecutedAt().toString());
        status.put("zoneId", controlLog.getZone().getZoneId());

        // AbnormalLog 정보 추가
        status.put("abnormalId", controlLog.getAbnormalLog().getId());
        status.put("abnormalType", controlLog.getAbnormalLog().getAbnormalType());
        status.put("targetType", controlLog.getAbnormalLog().getTargetType().name());
        status.put("targetId", controlLog.getAbnormalLog().getTargetId());

        // 발송 상태 추가
        status.putAll(deliveryStatus);

        messagingTemplate.convertAndSend("/topic/control-status", status);
    }
}
