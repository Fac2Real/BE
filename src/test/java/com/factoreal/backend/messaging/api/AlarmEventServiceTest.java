package com.factoreal.backend.messaging.api;

import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.application.ZoneService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.messaging.kafka.dto.WearableKafkaDto;
import com.factoreal.backend.messaging.kafka.strategy.NotificationStrategyFactory;
import com.factoreal.backend.messaging.kafka.strategy.alarmList.NotificationStrategy;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventResponse;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AlarmEventServiceTest {

    @Mock
    NotificationStrategyFactory notificationStrategyFactory;

    @Mock
    ZoneRepoService zoneRepoService;

    @Mock
    ZoneService zoneService;

    @Mock
    NotificationStrategy mockStrategy;

    @InjectMocks
    AlarmEventService alarmEventService;

    private AbnormalLog abnormalLog;
    private SensorKafkaDto sensorKafkaDto;
    private Zone zone;

    @BeforeEach
    void setUp() {
        zone = Zone.builder()
            .zoneId("zone-001")
            .zoneName("제어구역")
            .build();

        abnormalLog = AbnormalLog.builder()
            .id(1L)
            .abnormalType("온도 과열")
            .targetId("sensor-01")
            .zone(zone)
            .build();

        sensorKafkaDto = new SensorKafkaDto(
            "zone-001","equip-01","sensor-01",
            "temp",0.0,"EQUIPMENT",LocalDateTime.now().toString(),1);
    }

    @Test
    @DisplayName("startAlarm()이 정상적으로 전략을 실행한다")
    void testStartAlarmSuccess() {
        // given
        when(zoneRepoService.findById("zone-001")).thenReturn(zone);
        when(notificationStrategyFactory.getStrategiesForLevel(RiskLevel.WARNING))
            .thenReturn(List.of(mockStrategy));

        // when
        alarmEventService.startAlarm(sensorKafkaDto, abnormalLog, 1);

        // then
        verify(mockStrategy, times(1)).send(any(AlarmEventResponse.class));
    }

    @Test
    @DisplayName("startAlarm()에서 DTO 생성 도중 예외 발생 시 전략이 실행되지 않는다")
    void testStartAlarm_DtoCreationFails() {
        // given
        when(zoneRepoService.findById("zone-001"))
            .thenThrow(new RuntimeException("DB 오류"));

        // when
        alarmEventService.startAlarm(sensorKafkaDto, abnormalLog, 2);

        // then
        verify(mockStrategy, never()).send(any());
    }

    @Test
    @DisplayName("SensorKafkaDto 기반 generateAlarmDto() 테스트 - 설비 센서")
    void testGenerateAlarmDto_WithSensorKafkaDto() throws Exception {
        // given
        when(zoneRepoService.findById("zone-001")).thenReturn(zone);

        SensorKafkaDto dto = new SensorKafkaDto(
            "zone-001", "equip-02", "sensor-01",
            "temp", 30.0, "EQUIPMENT", LocalDateTime.now().toString(), 2);

        // when
        AlarmEventResponse result = alarmEventService.generateAlarmDto(dto, abnormalLog, RiskLevel.CRITICAL);

        // then
        assertNotNull(result);
        assertEquals("sensor-01", result.getSensorId());
        assertEquals("equip-02", result.getEquipId());
        assertEquals("zone-001", result.getZoneId());
        assertEquals("temp", result.getSensorType()); // enum의 name()으로 저장됨
        assertEquals("온도 과열", result.getMessageBody());
        assertEquals("설비 센서", result.getSource());
        assertEquals("제어구역", result.getZoneName());
        assertEquals(RiskLevel.CRITICAL, result.getRiskLevel());
    }

    @Test
    @DisplayName("WearablerKafkaDto 기반 generateAlarmDto() 테스트 - 설비 센서")
    void testGenerateAlarmDto_WithWearableKafkaDto() throws Exception {
        // given
//        when(zoneRepoService.findById("zone-001")).thenReturn(zone);

        WearableKafkaDto dto = new WearableKafkaDto(
            "wearable-001", "worker-01", "temp",
            130L, 2, LocalDateTime.now().toString());

        // when
        AlarmEventResponse result = alarmEventService.generateAlarmDto(dto, abnormalLog, RiskLevel.CRITICAL);

        // then
        assertNotNull(result);
        assertEquals("wearable-001", result.getSensorId());
        assertEquals("zone-001", result.getEquipId());
        assertEquals(WearableDataType.heartRate.name(), result.getSensorType()); // enum의 name()으로 저장됨
        assertEquals("온도 과열", result.getMessageBody());
        assertEquals("웨어러블", result.getSource());
        assertEquals("제어구역", result.getZoneName());
        assertEquals(RiskLevel.CRITICAL, result.getRiskLevel());
    }
}
