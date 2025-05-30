package com.factoreal.backend.messaging.kafka.processor;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.stateStore.InMemoryZoneSensorStateStore;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.messaging.service.AlarmEventService;
import com.factoreal.backend.messaging.service.AutoControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SensorEventProcessorTest {

    AutoControlService auto = mock(AutoControlService.class);
    AbnormalLogService abnormalSvc = mock(AbnormalLogService.class);
    AbnormalLogRepoService repoSvc = mock(AbnormalLogRepoService.class);
    AlarmEventService alarmSvc = mock(AlarmEventService.class);
    WebSocketSender ws = mock(WebSocketSender.class);

    InMemoryZoneSensorStateStore store = new InMemoryZoneSensorStateStore();
    SensorEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SensorEventProcessor(
                auto, abnormalSvc, repoSvc, alarmSvc, ws, store);
    }

    @Test
    @DisplayName("위험도가 변경되면 saveAbnormalLog·startAlarm 호출")
    void process_shouldTriggerAlarm_whenRiskLevelChanged() throws Exception {
        // ── arrange ───────────────────────────────────────────
        String zoneId = "Z1";
        String sensorId = "S1";

        // 기존 센서 상태: WARNING(1)
        store.setSensorRiskLevel(zoneId, sensorId, RiskLevel.WARNING);

        SensorKafkaDto dto = new SensorKafkaDto();
        dto.setZoneId(zoneId);
        dto.setSensorId(sensorId);
        dto.setEquipId(zoneId);          // ENVIRONMENT 토픽 조건
        dto.setSensorType("temp");       // use literal matching expected code
        dto.setDangerLevel(2);           // CRITICAL

        // saveAbnormalLog stub
        when(abnormalSvc.saveAbnormalLogFromSensorKafkaDto(
                any(), any(), any(), any()))
                .thenReturn(mock(AbnormalLog.class));

        // ── act ───────────────────────────────────────────────
        processor.process(dto, "ENVIRONMENT");

        // ── assert ────────────────────────────────────────────
        // 1) 위험도 변경 → saveAbnormalLog 호출
        verify(abnormalSvc).saveAbnormalLogFromSensorKafkaDto(
                eq(dto), eq(SensorType.temp), eq(RiskLevel.CRITICAL), eq(TargetType.Sensor));

        // 2) startAlarm 호출
        verify(alarmSvc).startAlarm(eq(dto), any(AbnormalLog.class), eq(2));

        // 3) zoneRiskLevel 이 CRITICAL 로 업데이트됐는지 확인
        assertThat(store.getZoneRiskLevel(zoneId)).isEqualTo(RiskLevel.CRITICAL);
    }
}