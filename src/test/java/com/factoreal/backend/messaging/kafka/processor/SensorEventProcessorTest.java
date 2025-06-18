package com.factoreal.backend.messaging.kafka.processor;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.state.store.InMemoryZoneSensorStateStore;
import com.factoreal.backend.domain.state.store.InMemoryZoneWorkerStateStore;
import com.factoreal.backend.domain.state.store.ZoneSensorStateStore;
import com.factoreal.backend.domain.state.store.ZoneWorkerStateStore;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import com.factoreal.backend.messaging.kafka.strategy.alarmMessage.RiskMessageProvider;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.messaging.api.AlarmEventService;
import com.factoreal.backend.messaging.api.AutoControlService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorEventProcessorTest {


    AutoControlService auto = mock(AutoControlService.class);
    AbnormalLogService abnormalSvc = mock(AbnormalLogService.class);
    AbnormalLogRepoService repoSvc = mock(AbnormalLogRepoService.class);
    AlarmEventService alarmSvc = mock(AlarmEventService.class);
    WebSocketSender ws = mock(WebSocketSender.class);
    SensorRepoService sensorRepoService = mock(SensorRepoService.class);

    ZoneSensorStateStore store = new InMemoryZoneSensorStateStore(sensorRepoService);
    ZoneWorkerStateStore workerStateStore = new InMemoryZoneWorkerStateStore();
    SensorEventProcessor processor;
    ZoneRepoService zoneRepoService = mock(ZoneRepoService.class);
    RiskMessageProvider riskMessageProvider = mock(RiskMessageProvider.class);

    @BeforeEach
    void setUp() {
        processor = new SensorEventProcessor(
                auto, abnormalSvc, repoSvc, alarmSvc, ws, store, workerStateStore, zoneRepoService, sensorRepoService, riskMessageProvider);
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
        dto.setTime("2025-06-16T00:00:00");

        // saveAbnormalLog stub
        when(zoneRepoService.findById(zoneId)).thenReturn(mock(Zone.class));
        when(sensorRepoService.findById(sensorId)).thenReturn(mock(Sensor.class));
        when(abnormalSvc.saveAbnormalLog(any(AbnormalLog.class)))
                .thenReturn(mock(AbnormalLog.class));

        // ── act ───────────────────────────────────────────────
        processor.process(dto, "ENVIRONMENT");

        // ── assert ────────────────────────────────────────────
        // 1) 위험도 변경 → saveAbnormalLog 호출
        verify(abnormalSvc).saveAbnormalLog(any(AbnormalLog.class));

        // 2) startAlarm 호출
        verify(alarmSvc).startAlarm(eq(dto), any(AbnormalLog.class), eq(2));

        // 3) zoneRiskLevel 이 CRITICAL 로 업데이트됐는지 확인
        assertThat(store.getZoneRiskLevel(zoneId)).isEqualTo(RiskLevel.CRITICAL);
    }

    @Nested
    @DisplayName("process 메서드 테스트")
    class processTest {
        @Test
        @DisplayName("case1. Zone이 잘못된 경우")
        void processFailedZoneNotExist() {
            // Arrange
            String zoneId = "Z1";
            String sensorId = "S1";

            SensorKafkaDto dto = new SensorKafkaDto();
            dto.setZoneId(zoneId);
            dto.setSensorId(sensorId);
            dto.setEquipId(zoneId);          // ENVIRONMENT 토픽 조건
            dto.setSensorType("temp");       // 센서 타입 지정
            dto.setDangerLevel(2);           // CRITICAL

            // Zone이 존재하지 않도록 설정
            when(zoneRepoService.findById(zoneId))
                    .thenThrow(new NotFoundException("공간을 찾을 수 없습니다: " + zoneId));

            // Act & Assert
            processor.process(dto, "ENVIRONMENT");


            // Verify (존재하지 않는 Zone에 대해 다른 메서드 호출이 없어야 함)
            verify(zoneRepoService).findById(zoneId); // findById 호출 확인
            verifyNoInteractions(sensorRepoService, abnormalSvc, alarmSvc, ws); // 다른 의존성이 호출되지 않음 확인
        }

        @Test
        @DisplayName("case2. Sensor가 잘못된 경우")
        void processFailedSensorNotExist() {
            // Arrange
            String zoneId = "Z1";
            String sensorId = "S1";

            SensorKafkaDto dto = new SensorKafkaDto();
            dto.setZoneId(zoneId);
            dto.setSensorId(sensorId);
            dto.setEquipId(zoneId);          // ENVIRONMENT 토픽 조건
            dto.setSensorType("temp");       // 센서 타입
            dto.setDangerLevel(2);           // CRITICAL

            // Zone은 유효하지만 Sensor가 존재하지 않도록 설정
            when(zoneRepoService.findById(zoneId))
                    .thenReturn(mock(Zone.class)); // Zone은 조회됨
            when(sensorRepoService.findById(sensorId))
                    .thenThrow(new NotFoundException("센서를 찾을 수 없습니다: " + sensorId));

            // Act & Assert
            processor.process(dto, "ENVIRONMENT");


            // Verify (Sensor 관련 호출 확인)
            verify(zoneRepoService).findById(zoneId);
            verify(sensorRepoService).findById(sensorId);
            verifyNoInteractions(abnormalSvc, alarmSvc, ws); // 다른 의존성이 호출되지 않음 확인
        }

        @Test
        @DisplayName("case3. RiskLevel에 변화가 없는 경우 (알람 호출 없음)")
        void processNoRiskLevelChange() {
            // Arrange
            String zoneId = "Z1";
            String sensorId = "S1";

            // 기존 RiskLevel: WARNING
            store.setSensorRiskLevel(zoneId, sensorId, RiskLevel.WARNING);

            SensorKafkaDto dto = new SensorKafkaDto();
            dto.setZoneId(zoneId);
            dto.setSensorId(sensorId);
            dto.setEquipId(zoneId);          // ENVIRONMENT 토픽 조건
            dto.setSensorType("temp");
            dto.setDangerLevel(1);           // 그대로 WARNING

            // Zone과 Sensor는 유효함
            when(zoneRepoService.findById(zoneId))
                    .thenReturn(mock(Zone.class));
            when(sensorRepoService.findById(sensorId))
                    .thenReturn(mock(Sensor.class));
            when(repoSvc.countByIsReadFalse()).thenReturn(3L);
            // Act
            processor.process(dto, "ENVIRONMENT");

            // Assert (위험 수준 변화 없음 확인)
            verify(zoneRepoService).findById(zoneId);
            verify(sensorRepoService).findById(sensorId);
            assertThat(store.getSensorRiskLevel(zoneId, sensorId)).isEqualTo(RiskLevel.WARNING);

            // Verify (알람 및 로그 변경 호출 없음)
            verifyNoInteractions(abnormalSvc, alarmSvc, auto);

            // Verify (webSocketSender)는 안읽은 알람 수 전송을 위해 호출
//            verify(ws).sendUnreadCount(3L);
        }

        @Test
        @DisplayName("case4. Environment 토픽이지만 equip과 zone id가 다른 경우")
        void processEnvironMentButEquipIdNEQZoneId() {
            // Arrange
            String zoneId = "Z1";
            String equipId = "E1";
            String sensorId = "S1";

            SensorKafkaDto dto = new SensorKafkaDto();
            dto.setZoneId(zoneId);
            dto.setSensorId(sensorId);
            dto.setEquipId(equipId);          // ENVIRONMENT 토픽 조건
            dto.setSensorType("temp");       // 센서 타입
            dto.setDangerLevel(2);           // CRITICAL
            processor.process(dto, "ENVIRONMENT");
            verifyNoInteractions(ws, abnormalSvc, auto, alarmSvc);
        }

        @Test
        @DisplayName("case5. 존의 현재 Sensor 위험도가 이전 Sensor 위험보다 작은 경우")
        void processCurrentDangerLevelLowerThanBefore() {
            // Arrange
            String zoneId = "Z1";
            String sensorId = "S1";
            Sensor sensor = Sensor.builder()
                    .sensorType(SensorType.humid)
                    .build();
            // 기존 센서 상태: WARNING(1)
            store.setSensorRiskLevel(zoneId, sensorId, RiskLevel.CRITICAL);
            when(zoneRepoService.findById(zoneId)).thenReturn(mock(Zone.class));
            when(sensorRepoService.findById(anyString())).thenReturn(sensor);

            SensorKafkaDto dto = new SensorKafkaDto();
            dto.setZoneId(zoneId);
            dto.setSensorId(sensorId);
            dto.setEquipId(zoneId);          // ENVIRONMENT 토픽 조건
            dto.setSensorType("temp");       // 센서 타입
            dto.setDangerLevel(1);           // CRITICAL
            processor.process(dto, "ENVIRONMENT");

            verify(ws).sendDangerLevel(
                    zoneId, SensorType.humid.name(), 1
            );
        }

        @Test
        @DisplayName("case6. 존의 현재 Sensor 위험도가 Worker 위험도보다 작고, 과거 Sensor 위험도 ...")
        void processSensorLTWorkerPastBig() {
            // Arrange
            String zoneId = "Z1";
            String sensorId = "S1";
            String workerId = "W1";
            Sensor sensor = Sensor.builder()
                    .sensorType(SensorType.humid)
                    .build();
            // 기존 센서 상태: WARNING(1)
            store.setSensorRiskLevel(zoneId, sensorId, RiskLevel.CRITICAL);
            workerStateStore.setWorkerRiskLevel(zoneId, workerId, RiskLevel.CRITICAL);
            when(zoneRepoService.findById(zoneId)).thenReturn(mock(Zone.class));
            when(sensorRepoService.findById(anyString())).thenReturn(sensor);

            SensorKafkaDto dto = new SensorKafkaDto();
            dto.setZoneId(zoneId);
            dto.setSensorId(sensorId);
            dto.setEquipId(zoneId);          // ENVIRONMENT 토픽 조건
            dto.setSensorType("temp");       // 센서 타입
            dto.setDangerLevel(1);           // CRITICAL
            processor.process(dto, "ENVIRONMENT");

            verify(ws).sendDangerLevel(
                    zoneId, WearableDataType.heartRate.name(), 2
            );
        }
    }
}