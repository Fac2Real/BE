package com.factoreal.backend.messaging.kafka.strategy.alarmMessage;


import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;

public interface RiskMessageProvider {
    String getRiskMessageBySensor(SensorType sensorType, RiskLevel riskLevel);
    String getRiskMessageByWearble(WearableDataType wearableDataType, RiskLevel riskLevel);
}
