package com.factoreal.backend.messaging.kafka.strategy.alarmMessage;


import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;

public interface RiskMessageProvider {
    String getRiskMessageBySensor(SensorType sensorType, RiskLevel riskLevel, Number value);
    String getRiskMessageByWearble(WearableDataType wearableDataType, RiskLevel riskLevel, Number value);
}
