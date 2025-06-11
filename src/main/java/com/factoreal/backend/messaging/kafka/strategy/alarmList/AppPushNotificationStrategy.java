package com.factoreal.backend.messaging.kafka.strategy.alarmList;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.notifyLog.dto.TriggerType;
import com.factoreal.backend.domain.worker.application.WorkerService;
import com.factoreal.backend.domain.worker.dto.response.WorkerInfoResponse;
import com.factoreal.backend.messaging.fcm.application.FCMService;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventResponse;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component("APP")
@RequiredArgsConstructor
public class AppPushNotificationStrategy implements NotificationStrategy {
    private final FCMService fcmService;
    private final WorkerService workerService;
    private final AbnormalLogRepoService abnormalLogRepoService;

    @Override
    public void send(AlarmEventResponse alarmEventResponse) {
        log.info("📲 App Push Notification Strategy.");
        // 1. 같은 공간에 있는 작업자에게 FCM 푸시 알람 전송
        List<WorkerInfoResponse> workerList = workerService.getWorkersByZoneId(alarmEventResponse.getZoneId());
        if(workerList.isEmpty()){
            return;
        }
        AbnormalLog abnormalLog = abnormalLogRepoService.findById(alarmEventResponse.getEventId());
        workerList.forEach(worker -> {
            fcmService.sendZoneSafety(
                    alarmEventResponse.getZoneId(),
                    alarmEventResponse.getRiskLevel().getPriority(),
                    TriggerType.AUTOMATIC,
                    LocalDateTime.parse(alarmEventResponse.getTime()),
                    abnormalLog
            );
        });
    }

    @Override
    public RiskLevel getSupportedLevel() {
        return RiskLevel.WARNING;
    }
}
