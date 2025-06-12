package com.factoreal.backend.domain.equip.application;

import com.factoreal.backend.domain.equip.dto.response.LatestMaintenancePredictionResponse;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.equip.entity.EquipHistory;
import com.factoreal.backend.messaging.slack.api.SlackEquipAlarmService;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipMaintenanceService {

    private final EquipRepoService equipRepoService;
    private final ZoneRepoService  zoneRepoService;
    private final EquipHistoryRepoService equipHistoryRepoService;
    private final SlackEquipAlarmService slackEquipAlarmService;
    private final RestTemplate restTemplate;

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    @Value("${fastapi.predict-endpoint}")
    private String predictEndpoint;

    /**
     * 특정 설비의 가장 최근 예상 점검일자 조회
     *
     * @param equipId 설비 ID
     * @param zoneId  공간 ID (검증용)
     * @return 가장 최근 예상 점검일자 정보
     */
    @Transactional(readOnly = true)
    public LatestMaintenancePredictionResponse getLatestMaintenancePrediction(String equipId, String zoneId) {
        // 1. 설비 정보 조회 및 zoneId 검증
        Equip equip = equipRepoService.findById(equipId);
        if (!equip.getZone().getZoneId().equals(zoneId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "설비가 해당 공간에 속하지 않습니다.");
        }

        // 2. 해당 설비의 가장 최근 미점검 이력 조회
        Optional<EquipHistory> latestUncheckedHistory = equipHistoryRepoService.findLatestUncheckedByEquipId(equipId);

        // 3. 미점검 이력이 없는 경우 가장 최근 이력 조회
        EquipHistory history = latestUncheckedHistory
                .orElseGet(() -> equipHistoryRepoService.findLatestByEquipId(equipId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "최근 예상 점검일자를 찾을 수 없습니다.")));

        // 4. 예상 점검일까지 남은 일수 계산
        long daysUntilMaintenance = slackEquipAlarmService.getDaysUntilMaintenance(history.getAccidentDate());

        // 5. 응답 DTO 생성 및 반환
        return LatestMaintenancePredictionResponse.fromEntity(equip, history, daysUntilMaintenance);
    }

    /**
     * 남은 일수를 기반으로 예상 점검일자 계산
     *
     * @param remainingDays ML 모델이 예측한 잔존 수명 (남은 일수)
     * @return 예상 점검일자
     */
    public LocalDate calculateExpectedMaintenanceDate(int remainingDays) {
        return LocalDate.now().plusDays(remainingDays);
    }

    /**
     * 새로운 예상 점검일자 처리
     */
    @Transactional
    public void processMaintenancePrediction(String equipId, LocalDate expectedMaintenanceDate) {
        Equip equip = equipRepoService.findById(equipId);
        Optional<EquipHistory> latestUncheckedHistory = equipHistoryRepoService.findLatestUncheckedByEquipId(equipId);

        // 예상 점검일자와 현재 날짜와의 차이 계산
        long daysUntilMaintenance = slackEquipAlarmService.getDaysUntilMaintenance(expectedMaintenanceDate);

        // Case 1: 첫 예측값이거나 모든 이력이 점검 완료된 경우
        if (latestUncheckedHistory.isEmpty()) {
            saveNewMaintenanceHistory(equip, expectedMaintenanceDate);
            log.info("설비 [{}] 첫 예상 점검일자 {}를 저장했습니다.", equip.getEquipName(), expectedMaintenanceDate);

            // 첫 예측값에 대한 슬랙 알림 발송
            try {
                slackEquipAlarmService.sendEquipmentMaintenanceAlert(
                        equip.getEquipName(),
                        equip.getZone().getZoneName(),
                        expectedMaintenanceDate,
                        daysUntilMaintenance
                );
                log.info("설비 [{}] (공간: {}) 첫 예상 점검일자에 대한 알림 발송 완료 (D-{})",
                        equip.getEquipName(),
                        equip.getZone().getZoneName(),
                        daysUntilMaintenance);
            } catch (IOException e) {
                log.error("설비 [{}] 첫 예상 점검일자 알림 발송 실패: {}", equip.getEquipName(), e.getMessage());
            }
            return;
        }

        EquipHistory currentHistory = latestUncheckedHistory.get();

        // Case 2: 기존 미점검 이력이 있는 경우
        // D-5보다 짧으면 새로운 예상 점검일자 무시
        if (daysUntilMaintenance < 5) {
            log.info("설비 [{}] 새로운 예상 점검일자가 D-5 이내여서 기존 예상 점검일자 {}를 유지합니다.",
                    equip.getEquipName(), currentHistory.getAccidentDate());
            return;
        }

        // 현재 시점과 더 가까운 예상 점검일자로 업데이트
        long daysUntilCurrentMaintenance = slackEquipAlarmService.getDaysUntilMaintenance(currentHistory.getAccidentDate());
        if (daysUntilMaintenance < daysUntilCurrentMaintenance) {
            currentHistory.setAccidentDate(expectedMaintenanceDate);
            equipHistoryRepoService.save(currentHistory);
            log.info("설비 [{}] 예상 점검일자를 {}에서 {}로 업데이트했습니다.",
                    equip.getEquipName(), currentHistory.getAccidentDate(), expectedMaintenanceDate);
        } else {
            log.info("설비 [{}] 새로운 예상 점검일자가 현재 예상일보다 더 멀어서 기존 예상 점검일자 {}를 유지합니다.",
                    equip.getEquipName(), currentHistory.getAccidentDate());
        }
    }

    private void saveNewMaintenanceHistory(Equip equip, LocalDate accidentDate) {
        EquipHistory newHistory = EquipHistory.builder()
                .equip(equip)
                .accidentDate(accidentDate)
                .checkDate(null)
                .build();
        equipHistoryRepoService.save(newHistory);
    }

}