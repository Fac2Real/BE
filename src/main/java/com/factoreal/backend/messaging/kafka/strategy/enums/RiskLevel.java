package com.factoreal.backend.messaging.kafka.strategy.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum RiskLevel {
    INFO(0,"정상"),
    WARNING(1,"주의"),    // 주의 단계
    CRITICAL(2,"위험");    // 위험 단계
    private final int priority;
    private final String koName;
    RiskLevel(int priority, String koName) {
        this.priority = priority;
        this.koName = koName;
    }
    public static List<RiskLevel> getUpTo(RiskLevel level){
        return Arrays.stream(values())
                .filter(risklevel -> risklevel.priority <= level.getPriority())
                .sorted(Comparator.comparingInt(RiskLevel::getPriority))
                .collect(Collectors.toList());
    }
    public static RiskLevel fromPriority(int priority) {
        return Arrays.stream(values())
                .filter(riskLevel -> riskLevel.priority == priority)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown priority: " + priority));
    }

}
