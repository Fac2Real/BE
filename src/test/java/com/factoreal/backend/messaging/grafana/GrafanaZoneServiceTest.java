package com.factoreal.backend.messaging.grafana;

import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrafanaZoneServiceTest {

    /* ────── MOCKS ─────────────────────────────────── */
    @Mock DashboardFactory  factory;
    @Mock GrafanaClient     client;
    @Mock SensorRepoService sensorRepo;

    @InjectMocks
    GrafanaZoneService service;

    /* 공통 테스트 상수 */
    private static final String GRAFANA_URL   = "https://gf";
    private static final String DATASOURCE_ID = "ds-uid";
    private static final int    ORG_ID        = 42;

    @BeforeEach
    void setFields() {
        // @Value 필드를 Reflection 으로 주입
        ReflectionTestUtils.setField(service, "grafanaUrl", GRAFANA_URL);
        ReflectionTestUtils.setField(service, "orgId", ORG_ID);
        ReflectionTestUtils.setField(service, "datasourceUid", DATASOURCE_ID);
    }

    /* ========== 1) 정상 시나리오 ========== */
    @Test @DisplayName("createDashboardUrls : 센서 2 개 정상 생성")
    void createDashboard_success() throws Exception {
        // given ① 센서 목록
        Sensor s1 = mock(Sensor.class);
        when(s1.getSensorId()).thenReturn("S01");
        when(s1.getSensorType()).thenReturn(SensorType.temp);

        Sensor s2 = mock(Sensor.class);
        when(s2.getSensorId()).thenReturn("S02");
        when(s2.getSensorType()).thenReturn(SensorType.humid);

        when(sensorRepo.findByZone_ZoneId("Z1"))
                .thenReturn(List.of(s1, s2));

        // given ② 대시보드 JSON & UID
        when(factory.build(eq("Z1"), anyList(), eq(DATASOURCE_ID)))
                .thenReturn("{json}");
        when(client.createDashboard("{json}"))
                .thenReturn("DUID");

        // when
        List<GrafanaSensorResponseDto> dtos = service.createDashboardUrls("Z1");

        // then
        assertThat(dtos).hasSize(2);

        // 첫 번째 DTO 검증
        GrafanaSensorResponseDto d1 = dtos.get(0);
        assertThat(d1.getSensorId()).isEqualTo("S01");
        assertThat(d1.getSensorType()).isEqualTo("TEMP");
        assertThat(d1.getIframeUrl()).isEqualTo(
                GRAFANA_URL + "/d-solo/DUID/S01?orgId=42&panelId=1&kiosk=tv&from=now-1h&to=now");

        // 두 번째 DTO 검증
        GrafanaSensorResponseDto d2 = dtos.get(1);
        assertThat(d2.getIframeUrl()).endsWith("panelId=2&kiosk=tv&from=now-1h&to=now");

        // 의존성 호출도 확인
        verify(factory).build("Z1", List.of(s1, s2), DATASOURCE_ID);
        verify(client).createDashboard("{json}");
    }

    /* ========== 2) 센서가 없으면 NotFoundException ========== */
    @Test @DisplayName("createDashboardUrls : 센서 없음 → NotFoundException")
    void createDashboard_noSensors() {
        when(sensorRepo.findByZone_ZoneId("Z1"))
                .thenReturn(List.of());                      // 빈 목록

        // Z1 센서가 존재하지 않기에(반환 데이터 없음)
        // 404 예외가 발생한다.
        assertThatThrownBy(() -> service.createDashboardUrls("Z1"))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(factory, client);              // 이후 호출 無
    }

    /* ========== 3) GrafanaClient 예외 → 그대로 전파 ========== */
    @Test @DisplayName("createDashboardUrls : Grafana API 실패 → 예외 전파")
    void createDashboard_grafanaError() throws Exception {
        // Z1에 대한 센서 목 데이터를 반환 형식적인 가정만 하는것임으로
        // 센서 데이터를 일일이 다 넣어줄 필요가 없음 ( sensorId / sensorType 등등.. )
        Sensor s = mock(Sensor.class);
        when(sensorRepo.findByZone_ZoneId("Z1"))
                .thenReturn(List.of(s));

        // 제이슨 형태의 페이로드 DTO 예외처리를 위한 내용이기에 아무 값이나 넣음 any~()
        when(factory.build(anyString(), anyList(), anyString()))
                .thenReturn("{json}");
        // 에외처리 로직
        when(client.createDashboard(anyString()))
                .thenThrow(new IllegalArgumentException("GF 500"));
        // 예외처리 정상작동 확인
        assertThatThrownBy(() -> service.createDashboardUrls("Z1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}