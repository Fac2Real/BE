package com.factoreal.backend.domain.state.store;

import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;

public interface ZoneSensorStateStore {
    RiskLevel getZoneRiskLevel(String zoneId);

    RiskLevel getSensorRiskLevel(String zoneId, String sensorId);

    Sensor getHighestRiskSensor(String zoneId);

    void setSensorRiskLevel(String zoneId, String sensorId, RiskLevel riskLevel);
}
