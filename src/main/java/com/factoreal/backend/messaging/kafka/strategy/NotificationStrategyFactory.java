package com.factoreal.backend.messaging.kafka.strategy;

import com.factoreal.backend.messaging.kafka.strategy.alarmList.NotificationStrategy;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component // 이 클래스가 Spring 컴포넌트로 자동 등록되도록 지정
public class NotificationStrategyFactory {

    // 각 RiskLevel에 대응되는 여러 개의 NotificationStrategy를 담는 Map
    private final Map<RiskLevel, List<NotificationStrategy>> strategyMap;

    /**
     * 생성자 주입을 통해 등록된 NotificationStrategy 구현체들을 받아서
     * 지원하는 RiskLevel 기준으로 그룹핑하여 Map에 저장
     * - 하나의 위험 수준(RiskLevel)에 여러 전략이 등록되어도 허용
     */
    public NotificationStrategyFactory(List<NotificationStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.groupingBy(NotificationStrategy::getSupportedLevel));
    }

    /**
     * @param level Kafka 메시지에서 전달된 메세지 기반으로 생성된 Enum 타입 객체
     * 입력된 위험 수준(level)까지 포함하는 모든 하위 수준의 전략들을 리스트로 반환
     * 예: level이 MEDIUM이면 LOW + MEDIUM 수준의 전략들을 모두 반환
     */
    public List<NotificationStrategy> getStrategiesForLevel(RiskLevel level) {
        return RiskLevel.getUpTo(level).stream() // level 이하의 RiskLevel 리스트를 가져옴
                .flatMap(riskLevel -> strategyMap.getOrDefault(riskLevel, List.of()).stream()) // 각 level에 해당하는 전략 리스트를 가져와 스트림으로 변환
                .collect(Collectors.toList()); // 모든 전략을 하나의 리스트로 결합
    }
}
