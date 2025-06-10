package com.factoreal.backend.messaging.sender;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.messaging.common.dto.SystemLogDto;
import com.factoreal.backend.messaging.common.dto.ZoneDangerDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketSenderTest {

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    AlarmEventDto alarmEventDto;
    @InjectMocks
    WebSocketSender webSocketSender;

    @Test
    void testSendDangerLevel() {
        // Given
        String zoneId = "zone1";
        String sensorType = "temperature";
        int level = 5;

        // When
        webSocketSender.sendDangerLevel(zoneId, sensorType, level);

        // Then
        ArgumentCaptor<ZoneDangerDto> captor = ArgumentCaptor.forClass(ZoneDangerDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/zone"), captor.capture());
        ZoneDangerDto capturedDto = captor.getValue();

        assertEquals(zoneId,        capturedDto.getZoneId());
        assertEquals(sensorType,    capturedDto.getSensorType());
        assertEquals(level,         capturedDto.getLevel());
    }


    @Test
    void testSendSystemLog() {
        // Given
        SystemLogDto logDto = new SystemLogDto(null,null,null,0,0,null);

        // When
        webSocketSender.sendSystemLog(logDto);

        // Then
        verify(messagingTemplate).convertAndSend("/topic/system-log", logDto);
    }

    @Test
    void testSendDangerAlarm() {
        // Given
//        alarmEventDto = new AlarmEventDto(null,
//            null,
//            null,
//            null,
//            null,
//            0.0,
//            RiskLevel.CRITICAL,
//            null,
//            null,
//            null,
//            null);

        // When
        webSocketSender.sendDangerAlarm(alarmEventDto);

        // Then
        verify(messagingTemplate).convertAndSend("/topic/alarm", alarmEventDto);
    }

    @Test
    void testSendUnreadCount() {
        // Given
        long count = 10;

        // When
        webSocketSender.sendUnreadCount(count);

        // Then
        verify(messagingTemplate).convertAndSend("/topic/unread-count", count);
    }

    @Test
    void testSendControlStatus_withRawValues() {
        // Given: ControlLog + AbnormalLog 세팅 (원시 값 그대로)
        ControlLog controlLog = new ControlLog();
        controlLog.setId(1L);
        controlLog.setControlType("START");
        controlLog.setControlVal(0.0);         // 이 값을 그대로 전송
        controlLog.setControlStat(1);          // 이 값을 그대로 전송
        controlLog.setZone(Zone.builder().zoneId("zone1").build());
        controlLog.setExecutedAt(LocalDateTime.of(2025, 6, 10, 14, 30));

        AbnormalLog abnormalLog = new AbnormalLog();
        abnormalLog.setId(11L);
        abnormalLog.setAbnormalType("ERROR");
        abnormalLog.setTargetType(TargetType.Sensor);
        abnormalLog.setTargetId("target-id");
        controlLog.setAbnormalLog(abnormalLog);

        Map<String, Boolean> deliveryStatus = new HashMap<>();
        deliveryStatus.put("messageSent", true);

        // When
        webSocketSender.sendControlStatus(controlLog, deliveryStatus);

        // Then: 실제로 convertAndSend()에 넘겨진 Map을 캡처해서 검증
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/control-status"), captor.capture());

        Map<String, Object> capturedStatus = captor.getValue();

        // - controlId
        assertEquals(1L, capturedStatus.get("controlId"));
        // - controlType
        assertEquals("START", capturedStatus.get("controlType"));
        // - controlValue (원시 값: Double 0.0)
        assertEquals(0.0, capturedStatus.get("controlValue"));
        // - controlStatus (원시 값: Integer 1)
        assertEquals(1, capturedStatus.get("controlStatus"));
        // - executedAt (toString() 한 결과)
        assertEquals("2025-06-10T14:30", capturedStatus.get("executedAt"));
        // - zoneId
        assertEquals("zone1", capturedStatus.get("zoneId"));

        // AbnormalLog 정보
        assertEquals(11L, capturedStatus.get("abnormalId"));
        assertEquals("ERROR", capturedStatus.get("abnormalType"));
        assertEquals("Sensor", capturedStatus.get("targetType"));  // TargetType.Sensor.name()
        assertEquals("target-id", capturedStatus.get("targetId"));

        // deliveryStatus
        assertEquals(true, capturedStatus.get("messageSent"));
    }

}