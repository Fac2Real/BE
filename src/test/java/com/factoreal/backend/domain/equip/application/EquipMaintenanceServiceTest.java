package com.factoreal.backend.domain.equip.application;

import com.factoreal.backend.domain.equip.dto.response.EquipInfoResponse;
import com.factoreal.backend.domain.equip.dto.response.MaintenancePredictionResponse;
import com.factoreal.backend.messaging.slack.service.SlackEquipAlarmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EquipMaintenanceServiceTest {

    @InjectMocks
    private EquipMaintenanceService equipMaintenanceService;

    @Mock
    private EquipRepoService equipRepoService;

    @Mock
    private SlackEquipAlarmService slackEquipAlarmService;

    @Mock
    private RestTemplate restTemplate;

    private final String fastApiBaseUrl = "http://localhost:8000";
    private final String predictEndpoint = "/predict-from-s3";

    @BeforeEach
    void setUp() {
        // @Value 어노테이션으로 주입되는 값을 테스트 환경에서 직접 설정
        ReflectionTestUtils.setField(equipMaintenanceService, "fastApiBaseUrl", fastApiBaseUrl);
        ReflectionTestUtils.setField(equipMaintenanceService, "predictEndpoint", predictEndpoint);
    }

    @Test
    void D5일때_알림이_발송된다() throws IOException {
        // given
        String equipId = "equip_001";
        String equipName = "설비1";
        LocalDate expectedDate = LocalDate.now().plusDays(5);  // D-5 상황 설정
        
        // 설비 목록 Mock
        EquipInfoResponse equipInfo = new EquipInfoResponse();
        equipInfo.setEquipId(equipId);
        equipInfo.setEquipName(equipName);
        given(equipRepoService.findAll()).willReturn(List.of(equipInfo));

        // FastAPI 응답 Mock
        MaintenancePredictionResponse predictionResponse = new MaintenancePredictionResponse();
        predictionResponse.setExpectedMaintenanceDate(expectedDate);
        given(restTemplate.getForEntity(
            eq(fastApiBaseUrl + predictEndpoint + "/" + equipId),
            eq(MaintenancePredictionResponse.class)))
            .willReturn(ResponseEntity.ok(predictionResponse));

        // D-5 체크 Mock
        given(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate)).willReturn(5L);
        given(slackEquipAlarmService.shouldSendAlert(expectedDate)).willReturn(true);

        // when
        equipMaintenanceService.checkMaintenanceDates();

        // then
        verify(slackEquipAlarmService, times(1))
            .sendEquipmentMaintenanceAlert(equipName, expectedDate, 5L);
    }

    @Test
    void D3일때_알림이_발송된다() throws IOException {
        // given
        String equipId = "equip_001";
        String equipName = "설비1";
        LocalDate expectedDate = LocalDate.now().plusDays(3);  // D-3 상황 설정
        
        // 설비 목록 Mock
        EquipInfoResponse equipInfo = new EquipInfoResponse();
        equipInfo.setEquipId(equipId);
        equipInfo.setEquipName(equipName);
        given(equipRepoService.findAll()).willReturn(List.of(equipInfo));

        // FastAPI 응답 Mock
        MaintenancePredictionResponse predictionResponse = new MaintenancePredictionResponse();
        predictionResponse.setExpectedMaintenanceDate(expectedDate);
        given(restTemplate.getForEntity(
            eq(fastApiBaseUrl + predictEndpoint + "/" + equipId),
            eq(MaintenancePredictionResponse.class)))
            .willReturn(ResponseEntity.ok(predictionResponse));

        // D-3 체크 Mock
        given(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate)).willReturn(3L);
        given(slackEquipAlarmService.shouldSendAlert(expectedDate)).willReturn(true);

        // when
        equipMaintenanceService.checkMaintenanceDates();

        // then
        verify(slackEquipAlarmService, times(1))
            .sendEquipmentMaintenanceAlert(equipName, expectedDate, 3L);
    }

    @Test
    void D4일때_알림이_발송되지_않는다() throws IOException {
        // given
        String equipId = "equip_001";
        String equipName = "설비1";
        LocalDate expectedDate = LocalDate.now().plusDays(4);  // D-4 상황 설정
        
        // 설비 목록 Mock
        EquipInfoResponse equipInfo = new EquipInfoResponse();
        equipInfo.setEquipId(equipId);
        equipInfo.setEquipName(equipName);
        given(equipRepoService.findAll()).willReturn(List.of(equipInfo));

        // FastAPI 응답 Mock
        MaintenancePredictionResponse predictionResponse = new MaintenancePredictionResponse();
        predictionResponse.setExpectedMaintenanceDate(expectedDate);
        given(restTemplate.getForEntity(
            eq(fastApiBaseUrl + predictEndpoint + "/" + equipId),
            eq(MaintenancePredictionResponse.class)))
            .willReturn(ResponseEntity.ok(predictionResponse));

        // D-4 체크 Mock
        given(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate)).willReturn(4L);
        given(slackEquipAlarmService.shouldSendAlert(expectedDate)).willReturn(false);

        // when
        equipMaintenanceService.checkMaintenanceDates();

        // then
        verify(slackEquipAlarmService, never()).sendEquipmentMaintenanceAlert(anyString(), any(LocalDate.class), eq(4L));
    }
} 