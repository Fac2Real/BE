package com.factoreal.backend.messaging.kafka.strategy.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Kafka Consumer에서 해당 객체 생성
 * 다양한 채널로 발송될 알람의 내용을 담는 표준 이벤트 객체
 */
@Builder
@Data
@Schema(description = "Kafka 알람 이벤트 DTO. 센서 및 시스템 이벤트에 대한 경보 정보를 포함합니다.")
public class AlarmEventResponse {

    // 1. 필수 공통 정보

    @Schema(description = "알람 이벤트 고유 ID (추적용)", example = "1001")
    private Long eventId;

    @Schema(description = "구역 ID", example = "zone-001")
    private String zoneId;

    @Schema(description = "장비 ID", example = "equip-xyz")
    private String equipId;

    @Schema(description = "센서 또는 웨어러블 ID", example = "sensor-abc")
    private String sensorId;

    @Schema(description = "센서 타입 또는 알람 종류", example = "HIGH_HEART_RATE")
    private String sensorType;

    @Schema(description = "센서에서 감지된 값", example = "145.0")
    private double sensorValue;

    @Schema(description = "위험 수준", implementation = RiskLevel.class)
    private RiskLevel riskLevel;

    @Schema(description = "알람 발생 시각 (ISO 8601)", example = "2025-05-25T12:34:56Z")
    private String time;

    // 3. 내용 정보

    @Schema(description = "알람 본문 메시지", example = "환자의 심박수가 위험 수준을 초과했습니다.")
    private String messageBody;

    // 4. 부가 정보

    @Schema(description = "알람 발생 출처 서비스", example = "WearableSensorService")
    private String source;

    @Schema(description = "구역 이름", example = "응급실 A구역")
    private String zoneName;
}
