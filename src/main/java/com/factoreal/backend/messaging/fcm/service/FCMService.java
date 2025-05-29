package com.factoreal.backend.messaging.fcm.service;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.equip.application.EquipRepoService;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.worker.application.WorkerRepoService;
import com.factoreal.backend.domain.worker.application.WorkerService;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.application.ZoneHistoryRepoService;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FCMService {
    private final FCMPushService fcmService;
    private final WorkerRepoService workerRepoService;
    private final ZoneHistoryRepoService zoneHistoryRepoService;
    private final AbnormalLogRepoService abnormalLogRepoService;
    private final EquipRepoService equipRepoService;
    private final ZoneRepoService zoneRepoService;

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
    public void sendEquipMaintain(String workerId, String equipId){
        // 1. Equip -> 설비 명, 위치 조회, Worker -> FCM 토큰 조회
        Equip equip = equipRepoService.findById(equipId);
        Worker worker = workerRepoService.findById(workerId);
        // TODO abnormalLog기반으로 최신 이상을 알려주기
        // 2. 문구 생성
        String title = "[확인] 수동 호출, 설비 점검 요청";
        String body = "%s의 %s설비 점검 요청합니다.".formatted(equip.getZone().getZoneName(),equip.getEquipName());

        fcmService.sendMessage(worker.getFcmToken(), title, body);

    }
    /**
     * 작업자 건강 도움용 알람
     */
    public void sendWorkerSafety(String helperWorkerId, String careNeedWorkerId){
        // 1. helper-> FCM 토큰 조회 careNeedWorker -> 이름, 위치, 상태 조회
        Worker helper = workerRepoService.findById(helperWorkerId);
        Worker careNeedWorker = workerRepoService.findById(careNeedWorkerId);
        ZoneHist zoneHist = zoneHistoryRepoService.getCurrentWorkerLocation(careNeedWorker.getWorkerId());
//        AbnormalLog abnormalLog = abnormalLogRepoService.find
        // 2. 문구 생성
        String title = "[긴급] 수동 호출, 인근 근로자 건강 이상";
        String body = "%s에 있는 작업자 %s씨의 건강 이상이 발견되었습니다. 지원바랍니다.".formatted(zoneHist.getZone().getZoneName(), careNeedWorker.getName());

        fcmService.sendMessage(helper.getFcmToken(), title, body);

    }

    /**
     * 공간 위험 알람
     */
    public void sendZoneSafety(String zoneId){
        // 1. helper-> FCM 토큰 조회 careNeedWorker -> 이름, 위치, 상태 조회
        Zone zone = zoneRepoService.findById(zoneId);
        List<ZoneHist> zoneHistList = zoneHistoryRepoService.getCurrentWorkersByZoneId(zoneId);
        List<Worker> workerList = zoneHistList.stream().map(
            ZoneHist::getWorker // zoneId에 있는 작업자 조회
        ).toList();
        // TODO 어떤 이상인지 알려주기
//        AbnormalLog abnormalLog = abnormalLogRepoService.find
        // 2. 문구 생성
        String title = "[주의] 수동 호출, 작업장 위험";
        String body = "%s에 있는 작업자들은 ... 수치가 높으므로 주의하세요.".formatted(zone.getZoneName());

        workerList.forEach(worker -> {
            fcmService.sendMessage(worker.getFcmToken(), title, body);
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
