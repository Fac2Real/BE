package com.factoreal.backend.domain.state.store;

import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;

public interface ZoneWorkerStateStore {
    RiskLevel getZoneRiskLevel(String zoneId);

    RiskLevel getWorkerRiskLevel(String workerId);

    String getZoneId(String workerId);

    void setWorkerRiskLevel(String zoneId, String workerId, RiskLevel riskLevel);
}
