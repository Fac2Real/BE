package com.factoreal.backend.messaging.kafka.strategy.alarmList;

import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("APP")
public class AppPushNotificationStrategy implements NotificationStrategy {

    // TODO FCM 전송 로직
    @Override
    public void send(AlarmEventDto alarmEventDto) {
        log.info("📲 App Push Notification Strategy.");
    }

    @Override
    public RiskLevel getSupportedLevel() {
        return RiskLevel.WARNING;
    }
}
