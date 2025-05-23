package com.factoreal.backend.messaging.kafka.strategy.alarmList;

import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;

public interface NotificationStrategy {
    void send(AlarmEventDto alarmEventDto);
    // 이 인터페이스를 상속받는 객체가 동작할 최소 위험 레벨을 설정
    // Kafka로 받아온 센서 데이터와 비교하기 위함.
    // 센서의 Danger Level보다 같거나 낮은 Strategy들이 반환되기 위함.
    RiskLevel getSupportedLevel();
}
