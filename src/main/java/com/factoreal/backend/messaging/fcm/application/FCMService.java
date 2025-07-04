package com.factoreal.backend.messaging.fcm.application;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.equip.application.EquipRepoService;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.notifyLog.application.NotifyLogService;
import com.factoreal.backend.domain.notifyLog.dto.TriggerType;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.worker.application.WorkerRepoService;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.application.ZoneHistoryRepoService;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.global.exception.dto.BadRequestException;
import com.factoreal.backend.messaging.fcm.dto.FCMResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.jdbc.Work;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    public FCMResponse sendEquipMaintain(String workerId, String equipId, String message) {
        // 1. Equip -> 설비 명, 위치 조회, Worker -> FCM 토큰 조회
        Equip equip = equipRepoService.findById(equipId);
        Worker worker = workerRepoService.findById(workerId);
        // TODO abnormalLog기반으로 최신 이상을 알려주기
        // 2. 문구 생성
        String title = "[확인] 수동 호출, 설비 점검 요청";
        String body = "%s의 %s설비 점검 요청합니다.".formatted(equip.getZone().getZoneName(), equip.getEquipName());
        if (message != null && !message.isBlank()) {
            body = body.concat("\n[관리자 메세지] %s".formatted(message));
        }
        FCMResponse fcmResponse = workerFcmTokenCheck(worker);
        if (fcmResponse != null) {
            return fcmResponse;
        }
        fcmService.sendMessage(worker.getFcmToken(), title, body)
            .thenAccept(messageId -> {
                notifyLogService.saveNotifyLogFromFCM(
                    worker.getWorkerId(), true, TriggerType.MANUAL, LocalDateTime.now(), null
                );
            })
            .exceptionally(ex -> {
                notifyLogService.saveNotifyLogFromFCM(
                    worker.getWorkerId(), false, TriggerType.MANUAL, LocalDateTime.now(), null
                );
                log.error("❌ 작업자{}의 fcm 토큰이 등록되지 않았거나 전송에 실패했습니다", workerId);
                return null;
            });
        return successSendWorker(worker);
    }

    /**
     * 작업자 건강 도움용 알람
     */
    public FCMResponse sendWorkerSafety(String helperWorkerId, String careNeedWorkerId,String message) {
        Worker helper = workerRepoService.findById(helperWorkerId);
        Worker careNeedWorker = workerRepoService.findById(careNeedWorkerId);
        if (helper.getWorkerId().equals(careNeedWorker.getWorkerId())) {
            throw new BadRequestException("❌ 도움이 필요한 작업자 자신에게 호출할 수 없습니다.");
        }
        ZoneHist zoneHist = zoneHistoryRepoService.getCurrentWorkerLocation(careNeedWorker.getWorkerId());
        String zoneName = (zoneHist == null || zoneHist.getZone() == null) ?
            DEFAULT_ZONE_NAME : zoneHist.getZone().getZoneName();

        String title = "[긴급] 수동 호출, 인근 근로자 건강 이상";
        String body = String.format("%s에 있는 작업자 %s씨의 건강 이상이 발견되었습니다. 지원바랍니다.",
            zoneName, careNeedWorker.getName());
        if (message != null && !message.isBlank()) {
            body = body.concat("\n[관리자 메세지] %s".formatted(message));
        }
        FCMResponse fcmResponse = workerFcmTokenCheck(helper);
        if (fcmResponse != null) {
            return fcmResponse; // 실패하면 FCMResponse에 객체가 생성되고, 해당 라인이 실행.
        }

        fcmService.sendMessage(helper.getFcmToken(), title, body)
            .thenAccept(messageId -> notifyLogService.saveNotifyLogFromFCM(
                helper.getWorkerId(), true, TriggerType.MANUAL, LocalDateTime.now(), null
            ))
            .exceptionally(ex -> {
                log.error("❌ 작업자 {}에게 FCM 전송 실패: {}", helper.getWorkerId(), ex.getMessage());
                notifyLogService.saveNotifyLogFromFCM(
                    helper.getWorkerId(), false, TriggerType.MANUAL, LocalDateTime.now(), null
                );
                return null;
            });
        return successSendWorker(helper);
    }


    /**
     * 공간 위험 알람
     */
    public List<FCMResponse> sendZoneSafety(
        String zoneId,
        Integer dangerLevel,
        TriggerType triggerType,
        LocalDateTime time,
        AbnormalLog abnormalLogOption
    ) {
        Zone zone = zoneRepoService.findById(zoneId);

        AbnormalLog abnormalLog = (abnormalLogOption == null) ?
            abnormalLogRepoService.findLatestSensorLogInZoneWithDangerLevel(TargetType.Sensor, zone, dangerLevel)
            : abnormalLogOption;

        Sensor sensor = sensorRepoService.getSensorById(abnormalLog.getTargetId());
        List<Worker> workerList = zoneHistoryRepoService.getCurrentWorkersByZoneId(zoneId).stream()
            .map(ZoneHist::getWorker)
            .toList();

        String title = "[주의] 수동 호출, 작업장 위험";
        String body = String.format("%s에 있는 작업자들은 %s 센서의 수치가 높으므로 주의하세요.",
            zone.getZoneName(), sensor.getSensorType());
        List<FCMResponse> sendResult = new ArrayList<>();

        for (Worker worker : workerList) {
            FCMResponse fcmResponse = workerFcmTokenCheck(worker);
            if (fcmResponse != null) {
                sendResult.add(fcmResponse); // 실패하면 FCMResponse에 객체가 생성되고, 해당 라인이 실행.
                continue;
            }

            fcmService.sendMessage(worker.getFcmToken(), title, body)
                .thenAccept(messageId -> notifyLogService.saveNotifyLogFromFCM(
                    worker.getWorkerId(), true, triggerType, time, abnormalLog.getId()
                ))
                .exceptionally(ex -> {
                    log.error("❌ 작업자 {}에게 FCM 전송 실패: {}", worker.getWorkerId(), ex.getMessage());
                    notifyLogService.saveNotifyLogFromFCM(
                        worker.getWorkerId(), false, triggerType, time, abnormalLog.getId()
                    );
                    return null;
                });
            sendResult.add(successSendWorker(worker)); // private 메서드로 정의한 코드 재사용
        }
        return sendResult;
    }

    public FCMResponse sendCustomMessage(String workerId, String message) {

        // 1. 메세지를 보낼 대상 만들기
        // 1-1. 전달받은 workerId 목록으로 Worker 목록 조회
        Worker worker = workerRepoService.findById(workerId);

        String title = "[관리자 송신 알림]";
        if (message == null || message.isBlank()) {
            throw new BadRequestException("메세지를 입력해주세요.");
        }
        String body = "[관리자 메세지] %s".formatted(message);

        FCMResponse fcmResponse = workerFcmTokenCheck(worker);
        if (fcmResponse != null) {
            return fcmResponse; // 실패하면 FCMResponse에 객체가 생성되고, 해당 라인이 실행.
        }

        fcmService.sendMessage(worker.getFcmToken(), title, body)
            .thenAccept(messageId -> notifyLogService.saveNotifyLogFromFCM(
                worker.getWorkerId(), true, TriggerType.MANUAL, LocalDateTime.now(), null
            ))
            .exceptionally(ex -> {
                log.error("❌ Failed to send FCM message to worker {}: {}", worker.getWorkerId(), ex.getMessage());
                notifyLogService.saveNotifyLogFromFCM(
                    worker.getWorkerId(), false, TriggerType.MANUAL, LocalDateTime.now(), null
                );
                return null;
            });

        return successSendWorker(worker);
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
    private FCMResponse workerFcmTokenCheck(Worker worker){
        String fcmToken = worker.getFcmToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("❌ 작업자 {}의 FCM 토큰이 없습니다. 알림 전송 건너뜀", worker.getWorkerId());
            notifyLogService.saveNotifyLogFromFCM(
                worker.getWorkerId(), false, TriggerType.MANUAL, LocalDateTime.now(), null
            );
            return FCMResponse.builder()
                .workerId(worker.getWorkerId())
                .workerName(worker.getName())
                .success(false)
                .errorDescription("❌ 작업자의 FCM 토큰이 없습니다.")
                .build();
        }
        return null;
    }

    private FCMResponse successSendWorker(Worker worker){
        return FCMResponse.builder()
            .workerId(worker.getWorkerId())
            .workerName(worker.getName())
            .success(true)
            .build();
    }
}
