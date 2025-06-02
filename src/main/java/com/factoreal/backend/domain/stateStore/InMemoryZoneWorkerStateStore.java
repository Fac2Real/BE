package com.factoreal.backend.domain.stateStore;

import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicIntegerArray;

@Slf4j
@Component
public class InMemoryZoneWorkerStateStore implements ZoneWorkerStateStore {
    private final ConcurrentMap<String, RiskLevel> workerRiskLevels = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicIntegerArray> zoneStateCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> workerZoneStates = new ConcurrentHashMap<>();
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
    public RiskLevel getWorkerRiskLevel(String workerId) {
        RiskLevel workerRiskLevel = workerRiskLevels.get(workerId);
        if (workerRiskLevel == null) {
            return RiskLevel.INFO;
        }

        return workerRiskLevel;
    }

    @Override
    public String getZoneId(String workerId) {
        return workerZoneStates.get(workerId);
    }

    @Override
    public void setWorkerRiskLevel(String zoneId, String workerId, RiskLevel riskLevel) {
        // 1) zoneStateCounts에 zoneId 없으면 생성
        if (!zoneStateCounts.containsKey(zoneId)) {
            zoneStateCounts.put(zoneId, new AtomicIntegerArray(PRIORITY_TO_LEVEL.length));
        }

        // 2) workerZoneStates에 workerId 없으면 생성, zoneId 불일치시 이동
        if (!workerZoneStates.containsKey(workerId)) {
            workerZoneStates.put(workerId, zoneId);
        } else if (!workerZoneStates.get(workerId).equals(zoneId)) {
            log.info("Zone 이동 {} -> {}", workerZoneStates.get(workerId), zoneId);
            String prevZoneId = workerZoneStates.get(workerId);
            if (prevZoneId != null) {
                zoneStateCounts.get(prevZoneId).decrementAndGet(workerRiskLevels.get(workerId).ordinal());
            }
            workerZoneStates.put(workerId, zoneId);
        }

        // 3) workerRiskLevels 설정, zoneStateCounts value 증감
        if (workerRiskLevels.containsKey(workerId)) {
            RiskLevel prevRiskLevel = workerRiskLevels.get(workerId);
            zoneStateCounts.get(zoneId).decrementAndGet(prevRiskLevel.ordinal());
        }
        workerRiskLevels.put(workerId, riskLevel);
        zoneStateCounts.get(zoneId).incrementAndGet(riskLevel.ordinal());
    }
}
