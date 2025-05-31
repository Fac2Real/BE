package com.factoreal.backend.domain.equip.application;

import com.factoreal.backend.domain.equip.dto.response.EquipInfoResponse;
import com.factoreal.backend.domain.equip.dto.response.MaintenancePredictionResponse;
import com.factoreal.backend.messaging.slack.service.SlackEquipAlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipMaintenanceService {

    private final EquipRepoService equipRepoService;
    private final SlackEquipAlarmService slackEquipAlarmService;
    private final RestTemplate restTemplate;

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    @Value("${fastapi.predict-endpoint}")
    private String predictEndpoint;

    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시에 실행
    public void checkMaintenanceDates() {
        log.info("설비 점검일 확인 작업 시작");

        List<EquipInfoResponse> equipments = equipRepoService.findAll();

        for (EquipInfoResponse equipment : equipments) {
            try {
                // FastAPI URL 생성 (query parameter 방식)
                String url = UriComponentsBuilder
                    .fromUriString(fastApiBaseUrl)
                    .path(predictEndpoint)
                    .queryParam("equipId", equipment.getEquipId())
                    .queryParam("zoneId", equipment.getZoneId())
                    .toUriString();

                log.info("Calling FastAPI with URL: {}", url);

                // FastAPI로부터 예상 점검일 조회
                ResponseEntity<MaintenancePredictionResponse> response = 
                    restTemplate.getForEntity(url, MaintenancePredictionResponse.class);

                // 예상 점검일이 있는 경우
                if (response.getBody() != null) {
                    LocalDate expectedMaintenanceDate = response.getBody().getExpectedMaintenanceDate();
                    // 예상 점검일과 현재 날짜의 차이 계산
                    long daysUntilMaintenance = slackEquipAlarmService.getDaysUntilMaintenance(expectedMaintenanceDate);

                    // 알림 전송 조건 확인
                    if (slackEquipAlarmService.shouldSendAlert(expectedMaintenanceDate)) {
                        slackEquipAlarmService.sendEquipmentMaintenanceAlert(
                            equipment.getEquipName(),
                            expectedMaintenanceDate,
                            daysUntilMaintenance
                        );
                        log.info("설비 [{}] 점검 알림 발송 완료 (D-{})", equipment.getEquipName(), daysUntilMaintenance);
                    }
                }
            } catch (IOException e) {
                log.error("설비 [{}] 점검 알림 발송 실패: {}", equipment.getEquipName(), e.getMessage());
            } catch (Exception e) {
                log.error("설비 [{}] 예상 점검일 조회 실패: {}", equipment.getEquipName(), e.getMessage());
            }
        }
    }
}