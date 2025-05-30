package com.factoreal.backend.domain.stateStore;

import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicIntegerArray;

@Component
public class InMemoryZoneWorkerStateStore implements ZoneWorkerStateStore {
    private final ConcurrentMap<String, AtomicIntegerArray> zoneStateCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentMap<String, RiskLevel>> zoneWorkerStates = new ConcurrentHashMap<>();
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
    public RiskLevel getWorkerRiskLevel(String zoneId, String workerId) {
        ConcurrentMap<String, RiskLevel> workerStates = zoneWorkerStates.get(zoneId);
        if (workerStates == null) {
            return RiskLevel.INFO;
        }

        return workerStates.getOrDefault(workerId, RiskLevel.INFO);
    }

    @Override
    public void setWorkerRiskLevel(String zoneId, String workerId, RiskLevel riskLevel) {
        // 1) zoneStateCounts, zoneWorkerStates에 zoneId 없으면 생성
        if (!zoneStateCounts.containsKey(zoneId)) {
            zoneStateCounts.put(zoneId, new AtomicIntegerArray(PRIORITY_TO_LEVEL.length));
        }
        if (!zoneWorkerStates.containsKey(zoneId)) {
            zoneWorkerStates.put(zoneId, new ConcurrentHashMap<>());
        }

        // 2) zoneWorkerStates -> zoneId에 workerId 없으면 생성
        if (zoneWorkerStates.get(zoneId).containsKey(workerId)) {
            RiskLevel prevState = zoneWorkerStates.get(zoneId).get(workerId);
            zoneStateCounts.get(zoneId).decrementAndGet(prevState.ordinal());
        }
        zoneStateCounts.get(zoneId).incrementAndGet(riskLevel.ordinal());
        zoneWorkerStates.get(zoneId).put(workerId, riskLevel);
    }

    @Override
    public void moveWorkerRiskLevel(String zoneId, String prevWorkerId, String nextWorkerId, RiskLevel riskLevel) {
        if (prevWorkerId != null && zoneWorkerStates.containsKey(zoneId) && zoneWorkerStates.get(zoneId).containsKey(prevWorkerId)) {
            RiskLevel prevWorkerState = zoneWorkerStates.get(zoneId).get(prevWorkerId);
            zoneStateCounts.get(zoneId).decrementAndGet(prevWorkerState.ordinal());
            zoneWorkerStates.get(zoneId).remove(prevWorkerId);
        }
        setWorkerRiskLevel(zoneId, nextWorkerId, riskLevel);
    }
}
