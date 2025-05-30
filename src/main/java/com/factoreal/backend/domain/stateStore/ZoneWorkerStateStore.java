package com.factoreal.backend.domain.stateStore;

import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;

public interface ZoneWorkerStateStore {
    RiskLevel getZoneRiskLevel(String zoneId);

    RiskLevel getWorkerRiskLevel(String zoneId, String workerId);

    void setWorkerRiskLevel(String zoneId, String workerId, RiskLevel riskLevel);

    void moveWorkerRiskLevel(String prevZoneId, String nextZoneId, String workerId, RiskLevel riskLevel);
}
