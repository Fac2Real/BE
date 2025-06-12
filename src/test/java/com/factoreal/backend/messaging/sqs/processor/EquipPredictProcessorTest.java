package com.factoreal.backend.messaging.sqs.processor;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.equip.application.EquipMaintenanceService;
import com.factoreal.backend.domain.equip.application.EquipRepoService;
import com.factoreal.backend.domain.equip.dto.response.MaintenancePredictionResponse;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.messaging.slack.api.SlackEquipAlarmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

/**
 * 설비 추론 프로세서 테스트
 */
@ExtendWith(MockitoExtension.class)
public class EquipPredictProcessorTest {

    @InjectMocks
    EquipPredictProcessor processor;
    @Mock
    ZoneRepoService zoneRepoService;
    @Mock
    EquipRepoService equipRepoService;
    @Mock
    RestTemplate restTemplate;
    @Mock
    SlackEquipAlarmService slackEquipAlarmService;
    @Mock
    EquipMaintenanceService equipMaintenanceService;
    @Mock
    AbnormalLogService abnormalLogService;

    private final String baseUrl = "http://localhost:8000";
    private final String endpoint = "/predict";

    // Zone : 생산라인A, 설비 : 로봇 암 1호기
    private String zoneId = "20250507165750-827";
    private String equipId = "'20250507171316-389'";
    private Zone zone;
    private Equip equip;

    @BeforeEach
    void setUp() {
        // (MockitoExtension 썼으면 openMocks 불필요)
        ReflectionTestUtils.setField(processor, "fastApiBaseUrl", baseUrl);
        ReflectionTestUtils.setField(processor, "predictEndpoint", endpoint);

        zone = Zone.builder().zoneId(zoneId).zoneName("생산 라인 A").build();
        equip = Equip.builder().equipId(equipId).equipName("로봇 암 1호기").zone(zone).build();
    }

    @Test
    @DisplayName("정상 흐름: FastAPI가 remainDays=4 반환 → processMaintenancePrediction 호출")
    void whenFastApiReturns_validPrediction_thenProcessAndAlertAndLog() throws IOException {
        // given
        // 🚩 각 테스트 안에서만 필요한 스텁을 선언
        given(zoneRepoService.findById(zoneId)).willReturn(zone);
        given(equipRepoService.findById(equipId)).willReturn(equip);

        MaintenancePredictionResponse apiResp = new MaintenancePredictionResponse();
        apiResp.setStatus("ok");
        apiResp.setPredictions(Collections.singletonList(4.0));

        given(zoneRepoService.findById(zoneId)).willReturn(zone);
        given(equipRepoService.findById(equipId)).willReturn(equip);
        String expectedUrl = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(endpoint)
                .queryParam("equipId", equipId)
                .queryParam("zoneId", zoneId)
                .toUriString();

        given(restTemplate.getForEntity(eq(expectedUrl),
                eq(MaintenancePredictionResponse.class)))
                .willReturn(ResponseEntity.ok(apiResp));
        given(equipMaintenanceService.calculateExpectedMaintenanceDate(4))
                .willReturn(LocalDate.now().plusDays(4));
        given(slackEquipAlarmService.getDaysUntilMaintenance(any())).willReturn(4L);
        given(slackEquipAlarmService.shouldSendAlert(any())).willReturn(true);

        // when
        processor.equipPredProcess(zoneId, equipId);

        // then
        then(equipMaintenanceService).should(times(1))
                .processMaintenancePrediction(eq(equipId), any(LocalDate.class));
        then(slackEquipAlarmService).should(times(1))
                .sendEquipmentMaintenanceAlert(eq("로봇 암 1호기"), eq("생산 라인 A"),  any(LocalDate.class), eq(4L));
        then(abnormalLogService).should(times(1))
                .saveEquipAbnormalLog(eq(zone), eq(equip), eq(4), eq(1)); // remainDays=4 → dangerLevel=1
    }

    @Test
    @DisplayName("FastAPI 예측값 없으면 아무 동작 안 함")
    void whenFastApiReturnsEmpty_thenSkip() {
        // given
        MaintenancePredictionResponse apiResp = new MaintenancePredictionResponse();
        apiResp.setStatus("ok");
        apiResp.setPredictions(Collections.emptyList());

        given(zoneRepoService.findById(zoneId)).willReturn(zone);
        given(equipRepoService.findById(equipId)).willReturn(equip);
        given(restTemplate.getForEntity(anyString(), eq(MaintenancePredictionResponse.class)))
                .willReturn(ResponseEntity.ok(apiResp));

        // when
        processor.equipPredProcess(zoneId, equipId);

        // then
        then(equipMaintenanceService).shouldHaveNoInteractions();
        then(slackEquipAlarmService).shouldHaveNoInteractions();
        then(abnormalLogService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 zone/equip 들어오면 로그만 남기고 리턴")
    void whenInvalidZoneOrEquip_thenSkipSilently() {
        // given: zone 조회만 실패하게 override
        given(zoneRepoService.findById(zoneId))
                .willThrow(new RuntimeException("not found"));

        // when
        processor.equipPredProcess(zoneId, equipId);

        // then: 아무 interaction 도 없어야 함
        then(restTemplate).shouldHaveNoInteractions();
        then(equipMaintenanceService).shouldHaveNoInteractions();
        then(slackEquipAlarmService).shouldHaveNoInteractions();
        then(abnormalLogService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("remainDays=2일 때 dangerLevel=2로 AbnormalLog 저장")
    void whenRemainDays2_thenDangerLevel2AbnormalLogSaved() throws IOException {
        // given
        MaintenancePredictionResponse apiResp = new MaintenancePredictionResponse();
        apiResp.setStatus("ok");
        apiResp.setPredictions(Collections.singletonList(2.0));  // remainDays = 2

        given(zoneRepoService.findById(zoneId)).willReturn(zone);
        given(equipRepoService.findById(equipId)).willReturn(equip);

        String expectedUrl = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(endpoint)
                .queryParam("equipId", equipId)
                .queryParam("zoneId", zoneId)
                .toUriString();
        given(restTemplate.getForEntity(eq(expectedUrl), eq(MaintenancePredictionResponse.class)))
                .willReturn(ResponseEntity.ok(apiResp));

        // calculateExpectedMaintenanceDate, getDaysUntilMaintenance, shouldSendAlert 동작도 모킹
        LocalDate predictedDate = LocalDate.now().plusDays(2);
        given(equipMaintenanceService.calculateExpectedMaintenanceDate(2))
                .willReturn(predictedDate);
        given(slackEquipAlarmService.getDaysUntilMaintenance(predictedDate))
                .willReturn(2L);
        given(slackEquipAlarmService.shouldSendAlert(predictedDate))
                .willReturn(true);

        // when
        processor.equipPredProcess(zoneId, equipId);

        // then
        // 1) processMaintenancePrediction 호출
        then(equipMaintenanceService).should().processMaintenancePrediction(eq(equipId), eq(predictedDate));
        // 2) slack 알림
        then(slackEquipAlarmService).should().sendEquipmentMaintenanceAlert(
                eq(equip.getEquipName()),
                eq(zone.getZoneName()),
                eq(predictedDate),
                eq(2L)
        );
        // 3) dangerLevel = 2 로 abnormalLog 저장
        then(abnormalLogService).should().saveEquipAbnormalLog(eq(zone), eq(equip), eq(2), eq(2));
    }

}
