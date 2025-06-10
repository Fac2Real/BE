package com.factoreal.backend.messaging.fcm.application;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.equip.application.EquipRepoService;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.notifyLog.dto.TriggerType;
import com.factoreal.backend.domain.notifyLog.application.NotifyLogService;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.worker.application.WorkerRepoService;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.application.ZoneHistoryRepoService;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.global.exception.dto.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FCMService {
    private final FCMPushService fcmService;
    private final WorkerRepoService workerRepoService;
    private final ZoneHistoryRepoService zoneHistoryRepoService;
    private final EquipRepoService equipRepoService;
    private final ZoneRepoService zoneRepoService;
    private final AbnormalLogService abnormalLogService;
    private final AbnormalLogRepoService abnormalLogRepoService;
    private final NotifyLogService notifyLogService;
    private final SensorRepoService sensorRepoService;
    public static final String DEFAULT_ZONE_NAME = "대기실";

    @Transactional
    public String saveToken(String workerId, String token) throws Exception {
        Worker worker = workerRepoService.findById(workerId);
        if (worker == null) {
            throw new Exception("Worker not found");
        }
        worker.setFcmToken(token);
        return workerRepoService.save(worker).getFcmToken();
    }


    /**
     * 유지 보수용 알람
     */
    public void sendEquipMaintain(String workerId, String equipId) {
        // 1. Equip -> 설비 명, 위치 조회, Worker -> FCM 토큰 조회
        Equip equip = equipRepoService.findById(equipId);
        Worker worker = workerRepoService.findById(workerId);
        // TODO abnormalLog기반으로 최신 이상을 알려주기
        // 2. 문구 생성
        String title = "[확인] 수동 호출, 설비 점검 요청";
        String body = "%s의 %s설비 점검 요청합니다.".formatted(equip.getZone().getZoneName(), equip.getEquipName());

        try {
            if (worker.getFcmToken() == null){
                throw new Exception("fcm 토큰 없음");
            }
            fcmService.sendMessage(worker.getFcmToken(), title, body).get();
            notifyLogService.saveNotifyLogFromFCM(
                    worker.getWorkerId(),
                    Boolean.TRUE,
                    TriggerType.MANUAL,
                    LocalDateTime.now(),
                    null
            );
        } catch (Exception e) {
            notifyLogService.saveNotifyLogFromFCM(
                    worker.getWorkerId(),
                    Boolean.FALSE,
                    TriggerType.MANUAL,
                    LocalDateTime.now(),
                    null
            );
            log.error("❌ 작업자의 fcm 토큰이 등록되지 않았습니다.");
            throw new BadRequestException("❌ 작업자의 fcm 토큰이 등록되지 않았습니다.");
        }
    }

    /**
     * 작업자 건강 도움용 알람
     */
    public void sendWorkerSafety(String helperWorkerId, String careNeedWorkerId) {
        // 1. helper-> FCM 토큰 조회 careNeedWorker -> 이름, 위치, 상태 조회
        Worker helper = workerRepoService.findById(helperWorkerId);
        Worker careNeedWorker = workerRepoService.findById(careNeedWorkerId);
        ZoneHist zoneHist = zoneHistoryRepoService.getCurrentWorkerLocation(careNeedWorker.getWorkerId());
        String zoneName;
        if (zoneHist == null || zoneHist.getZone() == null) {
            zoneName = DEFAULT_ZONE_NAME;
        } else {
            zoneName = zoneHist.getZone().getZoneName();
        }
//        AbnormalLog abnormalLog = abnormalLogRepoService.find
        // 2. 문구 생성
        String title = "[긴급] 수동 호출, 인근 근로자 건강 이상";
        String body = "%s에 있는 작업자 %s씨의 건강 이상이 발견되었습니다. 지원바랍니다.".formatted(zoneName, careNeedWorker.getName());

        try {
            fcmService.sendMessage(helper.getFcmToken(), title, body).get();
            notifyLogService.saveNotifyLogFromFCM(
                    helper.getWorkerId(),
                    Boolean.TRUE,
                    TriggerType.MANUAL,
                    LocalDateTime.now(),
                    null
            );
        } catch (Exception e) {
            notifyLogService.saveNotifyLogFromFCM(
                    helper.getWorkerId(),
                    Boolean.FALSE,
                    TriggerType.MANUAL,
                    LocalDateTime.now(),
                    null
            );
            log.error("❌ 작업자의 fcm 토큰이 등록되지 않았습니다.");
            throw new BadRequestException("❌ 작업자의 fcm 토큰이 등록되지 않았습니다.");
        }
    }

    /**
     * 공간 위험 알람
     */
    public void sendZoneSafety(
            String zoneId,
            Integer dangerLevel,
            TriggerType triggerType,
            LocalDateTime time,
            AbnormalLog abnormalLogOption
    ) {
        // 1. helper-> FCM 토큰 조회 careNeedWorker -> 이름, 위치, 상태 조회
        Zone zone = zoneRepoService.findById(zoneId);

        AbnormalLog abnormalLog;
        if (abnormalLogOption == null) {
            abnormalLog = abnormalLogRepoService.findLatestSensorLogInZoneWithDangerLevel(TargetType.Sensor, zone, dangerLevel);
        } else {
            abnormalLog = abnormalLogOption;
        }

        Sensor sensor = sensorRepoService.getSensorById(abnormalLog.getTargetId());
        List<ZoneHist> zoneHistList = zoneHistoryRepoService.getCurrentWorkersByZoneId(zoneId);
        List<Worker> workerList = zoneHistList.stream().map(
                ZoneHist::getWorker // zoneId에 있는 작업자 조회
        ).toList();
        // TODO 어떤 이상인지 알려주기
        // 2. 문구 생성
        String title = "[주의] 수동 호출, 작업장 위험";
        String body = "%s에 있는 작업자들은 %s 센서의 수치가 높으므로 주의하세요.".formatted(zone.getZoneName(), sensor.getSensorType());


        workerList.forEach(worker -> {
            try {
                fcmService.sendMessage(worker.getFcmToken(), title, body).get();
                notifyLogService.saveNotifyLogFromFCM(
                        worker.getWorkerId(),
                        Boolean.TRUE,
                        triggerType,
                        time,
                        abnormalLog.getId()
                );
            } catch (Exception e) {
                notifyLogService.saveNotifyLogFromFCM(
                        worker.getWorkerId(),
                        Boolean.FALSE,
                        triggerType,
                        time,
                        abnormalLog.getId()
                );
                log.error("❌ 작업자의 fcm 토큰이 등록되지 않았습니다.");
                throw new BadRequestException("❌ 작업자의 fcm 토큰이 등록되지 않았습니다.");
            }
        });
    }

    // TODO 재시도 로직 구현
//    private void retryWithBackoff(Message message, int maxRetries) {
//        int delay = 1000; // 초기 1초
//        for (int i = 0; i < maxRetries; i++) {
//            try {
//                Thread.sleep(delay);
//                firebaseMessaging.send(message);
//                log.info("재시도 {}회차 성공", i + 1);
//                return;
//            } catch (FirebaseMessagingException | InterruptedException e) {
//                if (!e.isRetryable()) {
//                    log.warn("재시도 불가능한 오류 발생, 중단");
//                    return;
//                }
//                delay *= 2;
//            }
//        }
//        log.error("최대 재시도 실패");
//    }

}
