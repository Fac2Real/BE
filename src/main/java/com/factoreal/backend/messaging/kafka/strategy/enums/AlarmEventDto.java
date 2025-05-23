package com.factoreal.backend.messaging.kafka.strategy.enums;

import lombok.Builder;
import lombok.Data;

/**
 * Kafka Consumer에서 해당 객체 생성
 * 다양한 채널로 발송될 알람의 내용을 담는 표준 이벤트 객체
 */
@Builder
@Data
public class AlarmEventDto {
    // 1. 필수 공통 정보
    private Long eventId;           // 알람 이벤트 고유 ID (추적용) -> BE에서 할당
    private String zoneId;
    private String equipId;
    private String sensorId;
    private String sensorType;       // 알람 종류 (예: "HIGH_HEART_RATE", "LOW_BATTERY", "SERVER_DOWN") - 분류 및 라우팅에 사용
    private double sensorValue; // 이상치 값
    private RiskLevel riskLevel; // 알람 심각도 (예: CRITICAL, WARNING, INFO) - 채널 선택, 표현 방식 결정에 사용

    private String time;      // 알람 발생 시각 -> BE에서 할당

    // 3. 내용 정보 (프로토콜별로 활용 방식이 다름)
//        String title,           // 알람 제목 (푸시 제목, 이메일 제목 등에 활용)
    private String messageBody;    // 알람 본문 (푸시 내용, SMS 내용, 이메일 본문 등에 활용)

    // 4. 부가 정보 (선택적)
    private String source;          // 알람 발생 출처 (예: "WearableSensorService", "BatchJobMonitor")
    private String zoneName;
}