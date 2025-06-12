package com.factoreal.backend.messaging.sqs.processor;


import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.equip.application.EquipMaintenanceService;
import com.factoreal.backend.domain.equip.application.EquipRepoService;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.messaging.slack.api.SlackEquipAlarmService;
import com.factoreal.backend.domain.equip.dto.response.MaintenancePredictionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class EquipPredictProcessor {

    private final ZoneRepoService zoneRepoService;
    private final EquipRepoService equipRepoService;
    private final RestTemplate restTemplate;
    private final SlackEquipAlarmService slackEquipAlarmService;
    private final EquipMaintenanceService equipMaintenanceService;
    private final AbnormalLogService abnormalLogService;


    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    @Value("${fastapi.predict-endpoint}")
    private String predictEndpoint;

    /**
     * // fastAPI 호출 프로세스
    1. zone, equip 여부 확인 후 잔존 수명 추론 (fastAPI)
    2. 점검 잔존수명 기준으로 알림 발송 (D-5, D-3)
    3. 점검 잔존수명 기준으로 이상치 저장 (abnormalLog)
     */
    public void equipPredProcess(String zoneId, String equipId){

        log.info("=========================");
        Zone zone;
        Equip equip;

        try {
            zone  = zoneRepoService.findById(zoneId);
            equip = equipRepoService.findById(equipId);
        } catch (Exception e) {
            log.warn("⚠️ 유효하지 않은 이벤트, 스킵 (zoneId={}, equipId={}): {}",
                    zoneId, equipId, e.getMessage());
            return;
        }

        log.info("🔍 [{}] 설비 추론 시작 - [equipId={}]", equip.getEquipName(), equipId);

        try {
            String url = UriComponentsBuilder
                    .fromUriString(fastApiBaseUrl)
                    .path(predictEndpoint)
                    .queryParam("equipId", equipId)
                    .queryParam("zoneId", zoneId)
                    .toUriString();

            log.info("💡FastAPI 호출 - URL: {}", url);
            ResponseEntity<MaintenancePredictionResponse> resp =
                    restTemplate.getForEntity(url, MaintenancePredictionResponse.class);

            if (resp.getBody() == null || resp.getBody().getPredictions().isEmpty()) {
                log.warn("⚠️ FastAPI 예측값 없음 (equipId={})", equipId);
                return;
            }


            int remainDays = resp.getBody().getPredictions().get(0).intValue();
            log.info("➡️ FastAPI 예측 결과 (remainDays={}일)", remainDays);

            // 남은 일수를 예상 점검일자로 변환
            LocalDate expectedMaintenanceDate = equipMaintenanceService.calculateExpectedMaintenanceDate(remainDays);

            // 예상 점검일자 처리 및 DB 저장
            equipMaintenanceService.processMaintenancePrediction(equipId, expectedMaintenanceDate);

            // 예상 점검일과 현재 날짜의 차이 계산
            long daysUntil = slackEquipAlarmService.getDaysUntilMaintenance(expectedMaintenanceDate);

            log.info("설비 [{}] 예측 결과 수신 - 잔존 수명: {}일, 예상 점검일: {}, D-{}",
                    equip.getEquipName(),
                    remainDays,
                    expectedMaintenanceDate,
                    daysUntil);

            // 알림 전송 조건 확인
            if (slackEquipAlarmService.shouldSendAlert(expectedMaintenanceDate)) {
                slackEquipAlarmService.sendEquipmentMaintenanceAlert(
                        equip.getEquipName(),
                        zone.getZoneName(),
                        expectedMaintenanceDate,
                        daysUntil
                );
                log.info("✅ 알림 전송 완료 (equipId={}, D-{})", equipId, daysUntil);
            }

            // 위험 레벨 산정 (remainDays <=3 → 레벨2, 3<remainDays<5 → 레벨1)
            int dangerLevel = remainDays <= 3 ? 2
                    : (remainDays < 5 ? 1
                    : 0);  // 0 이면 로그를 남기지 않음

            if (dangerLevel > 0) {
                abnormalLogService.saveEquipAbnormalLog(zone, equip, remainDays, dangerLevel);
            }


        } catch (IOException e) {
            log.error("설비 [{}] 점검 알림 발송 실패: {}", equip.getEquipName(), e.getMessage());
        } catch (Exception e) {
            log.error("설비 [{}] 예상 점검일 조회 실패: {}", equip.getEquipName(), e.getMessage());
        }

        log.info("설비 점검일 예측 데이터 수집 완료");
    }
}
