package com.factoreal.backend.domain.stateStore;

import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicIntegerArray;

@Component
public class InMemoryZoneSensorStateStore implements ZoneSensorStateStore {
    private final ConcurrentMap<String, AtomicIntegerArray> zoneStateCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentMap<String, RiskLevel>> zoneSensorStates = new ConcurrentHashMap<>();
    private static final RiskLevel[] PRIORITY_TO_LEVEL = RiskLevel.values();

    @Override
    public RiskLevel getZoneRiskLevel(String zoneId) {
        AtomicIntegerArray levelCount = zoneStateCounts.get(zoneId);
        if (levelCount == null) {
            return RiskLevel.INFO;
        }

        // 우선순위 높은 순서대로(거꾸로) 루프 돌기
        for (int p = levelCount.length() - 1; p >= 0; p--) {
            if (levelCount.get(p) > 0) {
                return PRIORITY_TO_LEVEL[p];
            }
        }

        return RiskLevel.INFO;
    }

    @Override
    public RiskLevel getSensorRiskLevel(String zoneId, String sensorId) {
        ConcurrentMap<String, RiskLevel> sensorStates = zoneSensorStates.get(zoneId);
        if (sensorStates == null) {
            return RiskLevel.INFO;
        }

        return sensorStates.getOrDefault(sensorId, RiskLevel.INFO);
    }

    @Override
    public void setSensorRiskLevel(String zoneId, String sensorId, RiskLevel riskLevel) {
        // 1) zoneStateCounts, zoneSensorStates에 zoneId 없으면 생성
        if (!zoneStateCounts.containsKey(zoneId)) {
            zoneStateCounts.put(zoneId, new AtomicIntegerArray(PRIORITY_TO_LEVEL.length));
        }
        if (!zoneSensorStates.containsKey(zoneId)) {
            zoneSensorStates.put(zoneId, new ConcurrentHashMap<>());
        }

        // 2) zoneSensorStates -> zoneId에 sensorId 없으면 생성
        if (zoneSensorStates.get(zoneId).containsKey(sensorId)) {
            RiskLevel prevState = zoneSensorStates.get(zoneId).get(sensorId);
            zoneStateCounts.get(zoneId).decrementAndGet(prevState.ordinal());
        }
        zoneStateCounts.get(zoneId).incrementAndGet(riskLevel.ordinal());
        zoneSensorStates.get(zoneId).put(sensorId, riskLevel);
    }
}
