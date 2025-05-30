package com.factoreal.backend.domain.stateStore;

import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;

public interface ZoneSensorStateStore {
    RiskLevel getZoneRiskLevel(String zoneId);

    RiskLevel getSensorRiskLevel(String zoneId, String sensorId);

    void setSensorRiskLevel(String zoneId, String sensorId, RiskLevel riskLevel);
}
