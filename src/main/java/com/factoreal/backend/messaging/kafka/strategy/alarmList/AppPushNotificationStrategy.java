package com.factoreal.backend.messaging.kafka.strategy.alarmList;

import com.factoreal.backend.domain.worker.application.WorkerService;
import com.factoreal.backend.domain.worker.dto.response.WorkerInfoResponse;
import com.factoreal.backend.messaging.fcm.service.FCMService;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("APP")
@RequiredArgsConstructor
public class AppPushNotificationStrategy implements NotificationStrategy {
    private final FCMService fcmService;
    private final WorkerService workerService;
    // TODO FCM 전송 로직
    @Override
    public void send(AlarmEventDto alarmEventDto) {
        log.info("📲 App Push Notification Strategy.");
        // FCM 작업중 자러감...
        // 같은 공간에 있는 작업자에게 FCM 푸시 알람 전송
        List<WorkerInfoResponse> workerList = workerService.getWorkersByZoneId(alarmEventDto.getZoneId());
        workerList.forEach(worker -> {
            try {
                fcmService.sendMessage(worker.getFcmToken(), alarmEventDto.getZoneName(),alarmEventDto.getMessageBody());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public RiskLevel getSupportedLevel() {
        return RiskLevel.WARNING;
    }
}
