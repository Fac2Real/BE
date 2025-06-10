package com.factoreal.backend.domain.equip.application;

import com.factoreal.backend.domain.equip.dto.response.EquipPredTargetResponse;
import com.factoreal.backend.domain.equip.dto.response.MaintenancePredictionResponse;
import com.factoreal.backend.domain.equip.dto.response.LatestMaintenancePredictionResponse;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.equip.entity.EquipHistory;
import com.factoreal.backend.messaging.slack.api.SlackEquipAlarmService;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private LocalDate calculateExpectedMaintenanceDate(int remainingDays) {
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

    @Scheduled(cron = "6 0 13/1 * * *") // 매일 13:00:06부터 1시간 간격으로 실행
    public void fetchAndProcessMaintenancePredictions() {
        log.info("🔄 설비 점검일 예측 데이터 수집 시작");

        // 1. 엔티티 리스트 조회
        List<Equip> equips = equipRepoService.findEquipsWhereEquipIdNotEqualsZoneId();

        // 2. DTO로 변환
        List<EquipPredTargetResponse> inferenceTargets = equips.stream().map(equip -> {
            try{
                // zone 정보가 없으면 예외 발생 (NPE)
                Zone zone = zoneRepoService.findById(equip.getZone().getZoneId());
                return EquipPredTargetResponse.fromEntity(equip, zone);
            }catch (NotFoundException e){
                // 존재하지 않는 zoneId: 로그만 남기고 null 반환
                log.warn("📌 존재하지 않는 zoneId: {} (설비: {})", equip.getZone().getZoneId(), equip.getEquipId());
                return null;
            }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());


        log.info("🐤 추론 시작할 설비 : {}", inferenceTargets.stream().toList());

        for (EquipPredTargetResponse equipment : inferenceTargets) {
            // 개별 설비 정보 로그
            log.info("=========================");
            log.info("🔍 [{}] 설비 추론 시작 - [equipId: {}]",equipment.getEquipName(),equipment.getEquipId());

            try {
                // FastAPI URL 생성 (query parameter 방식)
                String url = UriComponentsBuilder
                        .fromUriString(fastApiBaseUrl)
                        .path(predictEndpoint)
                        .queryParam("equipId", equipment.getEquipId())
                        .queryParam("zoneId", equipment.getZoneId())
                        .toUriString();

                log.info("💡FastAPI 호출 - URL: {}, 설비: [{}]", url, equipment.getEquipName());

                // FastAPI로부터 예상 점검일 조회
                ResponseEntity<MaintenancePredictionResponse> response =
                        restTemplate.getForEntity(url, MaintenancePredictionResponse.class);

                log.info("➡️설비 점검 일자 추론 호출 결과 (FastAPI) : {}" , response);
                log.info("➡️설비 점검 일자 추론 결과 (FastAPI) : {}" , response.getBody().getPredictions().get(0));

                Integer remainDays = response.getBody().getPredictions().get(0).intValue();


                // 예상 점검일이 있는 경우
                if (response.getBody() != null && remainDays != null) {
                    // 남은 일수를 예상 점검일자로 변환
                    LocalDate expectedMaintenanceDate = calculateExpectedMaintenanceDate(remainDays);

                    // 예상 점검일자 처리 및 DB 저장
                    processMaintenancePrediction(equipment.getEquipId(), expectedMaintenanceDate);

                    // 예상 점검일과 현재 날짜의 차이 계산
                    long daysUntilMaintenance = slackEquipAlarmService.getDaysUntilMaintenance(expectedMaintenanceDate);

                    log.info("설비 [{}] 예측 결과 수신 - 잔존 수명: {}일, 예상 점검일: {}, D-{}",
                            equipment.getEquipName(),
                            remainDays,
                            expectedMaintenanceDate,
                            daysUntilMaintenance);

                    // 알림 전송 조건 확인
                    if (slackEquipAlarmService.shouldSendAlert(expectedMaintenanceDate)) {
                        slackEquipAlarmService.sendEquipmentMaintenanceAlert(
                                equipment.getEquipName(),
                                equipment.getZoneName(),
                                expectedMaintenanceDate,
                                daysUntilMaintenance
                        );
                        log.info("설비 [{}] (공간: {}) 점검 알림 발송 완료 (D-{})",
                                equipment.getEquipName(),
                                equipment.getZoneName(),
                                daysUntilMaintenance);
                    }
                }
            } catch (IOException e) {
                log.error("설비 [{}] 점검 알림 발송 실패: {}", equipment.getEquipName(), e.getMessage());
            } catch (Exception e) {
                log.error("설비 [{}] 예상 점검일 조회 실패: {}", equipment.getEquipName(), e.getMessage());
            }
        }

        log.info("설비 점검일 예측 데이터 수집 완료");
    }
}