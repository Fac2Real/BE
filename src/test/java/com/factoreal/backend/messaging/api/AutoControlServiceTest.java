package com.factoreal.backend.messaging.api;

import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.controlLog.application.ControlLogService;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AutoControlService 테스트 클래스
 * 센서값이 임계치를 벗어날 경우 자동 제어를 수행하는 서비스를 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class AutoControlServiceTest {

    @Mock
    private ControlLogService controlLogService;    // 제어 로그를 저장하는 서비스

    @Mock
    private SensorRepoService sensorRepoService;    // 센서 정보를 조회하는 서비스

    @InjectMocks
    private AutoControlService autoControlService;  // 테스트 대상 서비스

    @Captor
    private ArgumentCaptor<Zone> zoneCaptor;       // Zone 객체의 전달값을 캡처하기 위한 캡터

    private SensorKafkaDto sensorKafkaDto;         // 카프카로부터 수신한 센서 데이터
    private AbnormalLog abnormalLog;               // 비정상 상황 로그
    private Sensor sensor;                         // 센서 정보
    private Zone zone;                             // 구역 정보

    /**
     * 각 테스트 전에 실행되는 설정
     * 테스트에 필요한 기본 객체들을 초기화합니다.
     */
    @BeforeEach
    void setUp() {
        // 테스트용 Zone 데이터 생성
        zone = Zone.builder()
                .zoneId("ZONE001")
                .zoneName("테스트구역")
                .build();

        // 테스트용 Sensor 데이터 생성
        sensor = Sensor.builder()
                .sensorId("SENSOR001")
                .sensorType(SensorType.temp)        // 온도 센서
                .sensorThres(25.0)                  // 기준값 25도
                .allowVal(2.0)                      // 허용 오차 ±2도
                .zone(zone)
                .build();

        // 테스트용 SensorKafkaDto 생성 (카프카 메시지 모사)
        sensorKafkaDto = new SensorKafkaDto();
        sensorKafkaDto.setSensorId("SENSOR001");
        sensorKafkaDto.setVal(28.0);               // 기준값 초과

        // 테스트용 AbnormalLog 생성
        abnormalLog = new AbnormalLog();
        abnormalLog.setId(1L);
    }

    @Test
    @DisplayName("위험 수준이 0(정상)인 경우 제어 로직이 실행되지 않음")
    void whenDangerLevelIsZero_thenNoControl() {
        // when - 위험 수준 0으로 평가 실행
        autoControlService.evaluate(sensorKafkaDto, abnormalLog, 0);

        // then - 센서 조회 및 제어 로직이 실행되지 않음을 검증
        verify(sensorRepoService, never()).findById(any());
        verify(controlLogService, never()).saveControlLog(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("센서를 찾을 수 없을 때 제어하지 않음")
    void whenSensorNotFound_thenNoControl() {
        // given
        SensorKafkaDto dto = new SensorKafkaDto();
        dto.setSensorId("nonexistent");
        dto.setVal(25.0);

        AbnormalLog abnormalLog = mock(AbnormalLog.class);
        when(sensorRepoService.findById(anyString())).thenThrow(new NotFoundException("존재하지 않는 센서 ID: nonexistent"));

        // when & then
        assertThatThrownBy(() -> autoControlService.evaluate(dto, abnormalLog, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 센서 ID: nonexistent");

        verify(controlLogService, never()).saveControlLog(any(), any(), anyDouble(), anyInt(), any());
    }

    @Test
    @DisplayName("측정값이 허용 범위 내인 경우 제어 로직이 실행되지 않음")
    void whenValueInRange_thenNoControl() {
        // given - 허용 범위 내의 값으로 설정
        sensorKafkaDto.setVal(26.0);  // 허용 범위 내 (25 ± 2)
        given(sensorRepoService.findById(any())).willReturn(sensor);

        // when - 위험 수준 1로 평가 실행
        autoControlService.evaluate(sensorKafkaDto, abnormalLog, 1);

        // then - 제어 로직이 실행되지 않음을 검증
        verify(controlLogService, never()).saveControlLog(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("측정값이 허용 범위를 초과한 경우 제어 로직이 실행됨")
    void whenValueOutOfRange_thenControl() {
        // given - 허용 범위를 초과하는 값으로 설정
        sensorKafkaDto.setVal(28.0);  // 허용 범위 초과 (25 ± 2)
        given(sensorRepoService.findById(any())).willReturn(sensor);

        // when - 위험 수준 1로 평가 실행
        autoControlService.evaluate(sensorKafkaDto, abnormalLog, 1);

        // then - 제어 로직이 실행되었음을 검증
        verify(controlLogService, times(1))
                .saveControlLog(eq(abnormalLog), eq("에어컨"), eq(25.0), eq(1), eq(zone));
    }

    @Test
    @DisplayName("측정값이 허용 범위 미만인 경우 제어 로직이 실행됨")
    void whenValueBelowRange_thenControl() {
        // given - 허용 범위 미만의 값으로 설정
        sensorKafkaDto.setVal(22.0);  // 허용 범위 미만 (25 ± 2)
        given(sensorRepoService.findById(any())).willReturn(sensor);

        // when - 위험 수준 1로 평가 실행
        autoControlService.evaluate(sensorKafkaDto, abnormalLog, 1);

        // then - 제어 로직이 실행되었음을 검증
        verify(controlLogService, times(1))
                .saveControlLog(eq(abnormalLog), eq("에어컨"), eq(25.0), eq(1), eq(zone));
    }

    @Test
    @DisplayName("극단적인 측정값에 대한 처리 테스트")
    void testExtremeValues() {
        // given - 센서 조회 시 미리 생성한 센서 반환하도록 설정
        given(sensorRepoService.findById(any())).willReturn(sensor);

        // 1. 매우 큰 값 테스트
        sensorKafkaDto.setVal(Double.MAX_VALUE);
        autoControlService.evaluate(sensorKafkaDto, abnormalLog, 1);

        // 2. 매우 작은 값 테스트
        sensorKafkaDto.setVal(Double.MIN_VALUE);
        autoControlService.evaluate(sensorKafkaDto, abnormalLog, 1);

        // 3. 음수 값 테스트
        sensorKafkaDto.setVal(-100.0);
        autoControlService.evaluate(sensorKafkaDto, abnormalLog, 1);

        // then - 모든 케이스에 대해 제어 로직이 3번 실행되었음을 검증
        verify(controlLogService, times(3))
                .saveControlLog(eq(abnormalLog), eq("에어컨"), eq(25.0), eq(1), eq(zone));
    }

    @Test
    @DisplayName("SensorKafkaDto의 null 값 처리 테스트")
    void testNullValuesInSensorKafkaDto() {
        // given - sensorId만 설정하고 나머지 필드는 null인 DTO 생성
        SensorKafkaDto nullDto = new SensorKafkaDto();
        nullDto.setSensorId("SENSOR001");  // sensorId만 설정
        given(sensorRepoService.findById(any())).willReturn(sensor);

        // when - 대부분의 필드가 null인 DTO로 평가 실행
        autoControlService.evaluate(nullDto, abnormalLog, 1);

        // then - null 값으로 인해 제어 로직이 실행되지 않음을 검증
        verify(controlLogService, never()).saveControlLog(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("다양한 센서 타입에 대한 제어 타입 매핑 테스트")
    void testDifferentSensorTypeControlMapping() {
        // given - 센서 조회 시 미리 생성한 센서 반환하도록 설정
        given(sensorRepoService.findById(any())).willReturn(sensor);

        // 1. 온도 센서 테스트
        sensor.setSensorType(SensorType.temp);
        autoControlService.evaluate(sensorKafkaDto, abnormalLog, 1);
        verify(controlLogService, times(1))
                .saveControlLog(eq(abnormalLog), eq("에어컨"), any(), any(), any());
        reset(controlLogService);  // 호출 횟수 초기화

        // 2. 습도 센서 테스트
        sensor.setSensorType(SensorType.humid);
        autoControlService.evaluate(sensorKafkaDto, abnormalLog, 1);
        verify(controlLogService, times(1))
                .saveControlLog(eq(abnormalLog), eq("제습기"), any(), any(), any());
        reset(controlLogService);  // 호출 횟수 초기화

        // 3. 미세먼지 센서 테스트
        sensor.setSensorType(SensorType.dust);
        autoControlService.evaluate(sensorKafkaDto, abnormalLog, 1);
        verify(controlLogService, times(1))
                .saveControlLog(eq(abnormalLog), eq("공기청정기"), any(), any(), any());
        reset(controlLogService);  // 호출 횟수 초기화

        // 4. 기타 센서 테스트 (진동)
        sensor.setSensorType(SensorType.vibration);
        autoControlService.evaluate(sensorKafkaDto, abnormalLog, 1);
        verify(controlLogService, times(1))
                .saveControlLog(eq(abnormalLog), eq("vibration"), any(), any(), any());
    }
} 