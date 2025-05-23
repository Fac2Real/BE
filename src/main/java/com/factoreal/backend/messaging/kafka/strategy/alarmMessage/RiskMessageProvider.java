package com.factoreal.backend.messaging.kafka.strategy.alarmMessage;

import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;

public interface RiskMessageProvider {
    String getMessage(SensorType sensorType, RiskLevel riskLevel);
}
