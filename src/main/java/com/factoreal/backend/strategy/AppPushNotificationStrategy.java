package com.factoreal.backend.strategy;

import com.factoreal.backend.strategy.enums.AlarmEvent;
import com.factoreal.backend.strategy.enums.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("APP")
public class AppPushNotificationStrategy implements NotificationStrategy {


    @Override
    public void send(AlarmEvent alarmEvent) {
        log.info("📲 App Push Notification Strategy.");
    }

    @Override
    public RiskLevel getSupportedLevel() {
        return RiskLevel.WARNING;
    }
}
