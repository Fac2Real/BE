package com.factoreal.backend.domain.abnormalLog.application;


import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.request.AbnormalPagingRequest;
import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.sensor.dao.SensorRepository;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.state.store.InMemoryZoneWorkerStateStore;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import com.factoreal.backend.messaging.kafka.dto.WearableKafkaDto;
import com.factoreal.backend.messaging.kafka.strategy.alarmMessage.RiskMessageProvider;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbnormalLogServiceTest {

    @InjectMocks
    private AbnormalLogService abnormalLogService;

    @Mock
    private AbnormalLogRepoService abnormalLogRepoService;

    @Mock
    private RiskMessageProvider riskMessageProvider;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ZoneRepoService zoneRepoService;

    @Mock
    private InMemoryZoneWorkerStateStore inmemoryZoneWorkerStateStore;

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private SensorRepoService sensorRepoService;

    private SensorKafkaDto sensorKafkaDto;
    private WearableKafkaDto wearableKafkaDto;
    private Zone zone;
    private Equip equip;
    private AbnormalLog savedLog;
    private Sensor sensor;
    private AbnormalPagingRequest pagingRequest;
    private List<AbnormalLogResponse> abnormalLogResponses;

    @BeforeEach
    void setUp() {
        // Zone 설정
        zone = Zone.builder()
            .zoneId("ZONE001")
            .zoneName("테스트 구역")
            .build();

        // 센서 설정
        sensor = Sensor.builder()
            .sensorId("SENSOR001")
            .zone(zone)
            .sensorType(SensorType.temp)
            .build();

        // 센서 카프카 DTO 설정
        sensorKafkaDto = new SensorKafkaDto();
        sensorKafkaDto.setSensorId("SENSOR001");
        sensorKafkaDto.setZoneId(zone.getZoneId());
        sensorKafkaDto.setVal(35.5);
        sensorKafkaDto.setTime(String.valueOf(LocalDateTime.now()));

        // 웨어러블 카프카 DTO 설정
        wearableKafkaDto = new WearableKafkaDto();
        wearableKafkaDto.setWorkerId("WORKER001");
        wearableKafkaDto.setVal(120L);
        wearableKafkaDto.setTime(String.valueOf(LocalDateTime.now()));

        // 저장된 AbnormalLog 설정
        savedLog = AbnormalLog.builder()
            .id(1L)
            .targetType(TargetType.Sensor)
            .targetId("SENSOR001")
            .abnormalType("온도 위험")
            .abnVal(35.5)
            .dangerLevel(2)
            .detectedAt(LocalDateTime.now())
            .zone(zone)
            .isRead(false)
            .build();

        // 페이징 요청 설정
        pagingRequest = new AbnormalPagingRequest();
        pagingRequest.setPage(0);
        pagingRequest.setSize(10);

        // AbnormalLogResponse 리스트 설정
        abnormalLogResponses = new ArrayList<>();
        AbnormalLogResponse response = AbnormalLogResponse.from(savedLog);
        abnormalLogResponses.add(response);
    }

    @Nested
    @DisplayName("saveAbnormalLogFromSensorKafkaDto 메서드 테스트")
    class saveAbnormalLogFromSensorKafkaDtoTest {
        @Test
        @DisplayName("case 1: Kafka의 topic이 ENVIRONMENT일 때 정상 저장")
        void saveAbnormalLogFromSensorKafkaDtoTest_success() throws Exception {
            // given
//            when(sensorRepository.findById(anyString())).thenReturn(Optional.of(sensor));
            when(zoneRepoService.findById(anyString())).thenReturn(zone);
            when(riskMessageProvider.getRiskMessageBySensor(any(), any())).thenReturn("온도 위험");
            when(abnormalLogRepoService.save(any(AbnormalLog.class))).thenReturn(savedLog);

            // when
            AbnormalLog result = abnormalLogService.saveAbnormalLogFromSensorKafkaDto(
                sensorKafkaDto,
                SensorType.temp,
                RiskLevel.CRITICAL,
                TargetType.Sensor
            );

            // then
            assertNotNull(result);
            assertEquals(savedLog.getId(), result.getId());
            assertEquals(savedLog.getTargetType(), result.getTargetType());
            assertEquals(savedLog.getAbnormalType(), result.getAbnormalType());
            verify(abnormalLogRepoService, times(1)).save(any(AbnormalLog.class));
        }

        @Test
        @DisplayName("case2. 센서가 위치한 공간이 없을때 예외 검사")
        void saveAbnormalLogFromSensorKafkaDto_zoneNotFound_throwsException() {
            // given
//            when(sensorRepository.findById(anyString())).thenReturn(Optional.of(sensor));
            when(zoneRepoService.findById(anyString())).thenThrow(new NotFoundException("존재하지 않는 구역입니다."));

            // when & then
            assertThrows(NotFoundException.class, () -> {
                abnormalLogService.saveAbnormalLogFromSensorKafkaDto(
                    sensorKafkaDto,
                    SensorType.temp,
                    RiskLevel.CRITICAL,
                    TargetType.Sensor
                );
            });
        }
    }

    @Nested
    @DisplayName("saveAbnormalLogFromWearableKafkaDto 메서드 테스트")
    class saveAbnormalLogFromWearableKafkaDtoTest {
        @Test
        @DisplayName("case1. 웨어러블 데이터로부터 알람 로그 생성 성공")
        void saveAbnormalLogFromWearableKafkaDto_success() {
            // given
            when(zoneRepoService.findById(anyString())).thenReturn(zone);
            when(riskMessageProvider.getRiskMessageByWearble(any(), any())).thenReturn("심박수 위험");
            when(abnormalLogRepoService.save(any(AbnormalLog.class))).thenReturn(savedLog);
            when(inmemoryZoneWorkerStateStore.getZoneId(anyString())).thenReturn(zone.getZoneId());
            // when
            AbnormalLog result = abnormalLogService.saveAbnormalLogFromWearableKafkaDto(
                wearableKafkaDto,
                WearableDataType.heartRate,
                RiskLevel.CRITICAL,
                TargetType.Worker
            );

            // then
            assertNotNull(result);
            assertEquals(savedLog.getId(), result.getId());
            verify(abnormalLogRepoService, times(1)).save(any(AbnormalLog.class));
        }
    }

    @Test
    @DisplayName("findAllAbnormalLogs 모든 알람 로그 조회 성공")
    void findAllAbnormalLogs() {
        // given
        Page<AbnormalLog> abnormalLogPage = new PageImpl<>(List.of(savedLog));
        when(abnormalLogRepoService.findAll(any(Pageable.class))).thenReturn(abnormalLogPage);

        // when
        Page<AbnormalLogResponse> result = abnormalLogService.findAllAbnormalLogs(pagingRequest);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(abnormalLogResponses.get(0).getId(), result.getContent().get(0).getId());
    }

    @Test
    @DisplayName("findAllAbnormalLogsUnRead 읽지 않은 알람 로그 조회 성공")
    void findAllAbnormalLogsUnRead() {
        // given
        Page<AbnormalLog> abnormalLogPage = new PageImpl<>(List.of(savedLog));
        when(abnormalLogRepoService.findAllByIsReadIsFalseOrderByDetectedAtDesc(any(Pageable.class)))
            .thenReturn(abnormalLogPage);

        // when
        Page<AbnormalLogResponse> result = abnormalLogService.findAllAbnormalLogsUnRead(pagingRequest);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("findAbnormalLogsByAbnormalType 알람 유형별 로그 조회 성공")
    void findAbnormalLogsByAbnormalType() {
        // given
        Page<AbnormalLog> abnormalLogPage = new PageImpl<>(List.of(savedLog));
        when(abnormalLogRepoService.findAbnormalLogsByAbnormalType(any(Pageable.class), anyString()))
            .thenReturn(abnormalLogPage);

        // when
        Page<AbnormalLogResponse> result = abnormalLogService.findAbnormalLogsByAbnormalType(
            pagingRequest, "온도 위험");

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("온도 위험", result.getContent().get(0).getAbnormalType());
    }

    @Test
    @DisplayName("findLatestAbnormalLogsForTargets 타겟 대상별 최신 알람 로그 조회 성공")
    void findLatestAbnormalLogsForTargets() {
        // given
        List<String> targetIds = List.of("SENSOR001", "SENSOR002");
        when(abnormalLogRepoService.findFirstByTargetTypeAndTargetIdOrderByDetectedAtDesc(any(TargetType.class), anyString()))
            .thenReturn(Optional.of(savedLog));

        // when
        List<AbnormalLogResponse> result = abnormalLogService.findLatestAbnormalLogsForTargets(
            TargetType.Sensor, targetIds);

        // then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(targetIds.size(), result.size());
    }

    @Test
    @DisplayName("findAbnormalLogsByTargetId 특정 타겟의 알람 로그 조회 성공")
    void findAbnormalLogsByTargetId() {
        // given
        AbnormalLogResponse response = AbnormalLogResponse.builder()
            .targetId("SENSOR001")
            .build();

        // objectMapper가 AbnormalLog를 AbnormalLogResponse로 변환하도록 설정
        when(objectMapper.convertValue(any(AbnormalLog.class), eq(AbnormalLogResponse.class)))
            .thenReturn(response);

        Page<AbnormalLog> abnormalLogPage = new PageImpl<>(List.of(savedLog));
        when(abnormalLogRepoService.findAbnormalLogsByTargetTypeAndTargetId(
            any(Pageable.class), any(TargetType.class), anyString()))
            .thenReturn(abnormalLogPage);

        // when
        Page<AbnormalLogResponse> result = abnormalLogService.findAbnormalLogsByTargetId(
            pagingRequest, TargetType.Sensor, "SENSOR001");

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("SENSOR001", result.getContent().get(0).getTargetId());
    }


    @Test
    @DisplayName("readCheck 알람 읽음 상태 변경 성공")
    void readCheck() {
        // given
        AbnormalLog foundLog = AbnormalLog.builder()
            .id(1L)
            .isRead(false)
            .build();
        when(abnormalLogRepoService.findById(anyLong())).thenReturn(foundLog);
        when(abnormalLogRepoService.save(any(AbnormalLog.class))).thenReturn(
            AbnormalLog.builder().id(1L).isRead(true).build());

        // when
        boolean result = abnormalLogService.readCheck(1L);

        // then
        assertTrue(result);
        assertTrue(foundLog.getIsRead());
    }

    @Test
    @DisplayName("saveAbnormalLog 센서 알람 로그 저장 성공")
    void saveAbnormalLog() {
        // given
        String sensorId = "SENSOR001";
        String abnormalTypeMessage = "위험 메시지";
        Double valData = 25.5;
        String timeData = "2023-08-15T14:30:00";

        // sensorKafkaDto 설정
//        when(sensorKafkaDto.getSensorId()).thenReturn(sensorId);
//        when(sensorKafkaDto.getVal()).thenReturn(valData);
//        when(sensorKafkaDto.getTime()).thenReturn(timeData);

        // sensor 설정
        SensorType sensorType = SensorType.temp; // 예시 값, 실제 사용하는 타입으로 수정
//        when(sensor.getSensorType()).thenReturn(sensorType);
//        when(sensor.getZone()).thenReturn(zone);

        // riskMessageProvider 모킹 추가
        when(riskMessageProvider.getRiskMessageBySensor(any(SensorType.class), any(RiskLevel.class)))
            .thenReturn(abnormalTypeMessage);

//        when(zoneRepoService.findById(anyString())).thenReturn(zone);
        when(abnormalLogRepoService.save(any(AbnormalLog.class))).thenReturn(savedLog);

        // when
        AbnormalLog result = abnormalLogService.saveAbnormalLog(
            sensorKafkaDto, sensor, 2);

        // then
        assertNotNull(result);
        assertEquals(savedLog.getId(), result.getId());
        assertEquals(2, result.getDangerLevel());

        // 추가 검증
        verify(riskMessageProvider, times(1)).getRiskMessageBySensor(eq(sensorType), any(RiskLevel.class));
        verify(abnormalLogRepoService, times(1)).save(any(AbnormalLog.class));
    }

}