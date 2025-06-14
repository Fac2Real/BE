package com.factoreal.backend.messaging.kafka.strategy.alarmMessage;

import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import org.springframework.stereotype.Component;

/**
 * 위험 메시지를 센서 종류와 위험 수준에 따라 구성하는 기본 구현체
 * 각 센서 타입에 따라 적절한 단위를 포함한 메시지를 반환한다.
 */
@Component
public class DefaultRiskMessageProvider implements RiskMessageProvider {

    /**
     * 센서 타입과 위험 수준에 따라 경고 메시지를 생성하는 메소드
     *
     * @param sensorType 센서의 종류 (ex. 온도, 습도, 진동 등)
     * @param riskLevel 위험 수준 (INFO, WARNING, CRITICAL)
     * @param value 측정값 (숫자형)
     * @return 알림 메시지 (예: "45°C", "20 mm/s", "300 ppm")
     */
    @Override
    public String getRiskMessageBySensor(SensorType sensorType, RiskLevel riskLevel, Number value) {
        return switch (sensorType) {
            case temp -> switch (riskLevel) {
                case INFO, WARNING, CRITICAL -> "%s°C".formatted(value); // 온도 센서: 섭씨 단위 사용
            };
            case humid -> switch (riskLevel) {
                case INFO, WARNING, CRITICAL -> "%s%%".formatted(value); // 습도 센서: 백분율(%) 사용
            };
            case vibration -> switch (riskLevel) {
                case INFO, WARNING, CRITICAL -> "%s mm/s".formatted(value); // 진동 센서: mm/s (속도 기준)
            };
            case dust -> switch (riskLevel) {
                case INFO, WARNING, CRITICAL -> "%s ppm".formatted(value); // 먼지 농도: ppm 단위
            };
            case voc -> switch (riskLevel) {
                case INFO, WARNING, CRITICAL -> "%s ppm".formatted(value); // VOC(휘발성 유기화합물): ppm 단위
            };
            case current -> switch (riskLevel) {
                case INFO, WARNING, CRITICAL -> "%s A".formatted(value); // 전류: A(암페어)
            };
            case active_power -> switch (riskLevel) {
                case INFO, WARNING, CRITICAL -> "%s kW".formatted(value); // 유효전력: kW
            };
            case reactive_power -> switch (riskLevel) {
                case INFO, WARNING, CRITICAL -> "%s kVAR".formatted(value); // 무효전력: kVAR
            };
            case pressure -> switch (riskLevel) {
                case INFO, WARNING, CRITICAL -> "%s bar".formatted(value); // 압력: bar
            };
            default -> "등록되지 않은 센서입니다. 관리자에게 문의하세요."; // 정의되지 않은 센서 타입 처리
        };
    }

    /**
     * 웨어러블 장치 데이터 타입과 위험 수준에 따른 메시지를 생성하는 메소드
     *
     * @param wearableDataType 웨어러블 데이터 타입 (ex. 심박수)
     * @param riskLevel 위험 수준 (INFO, WARNING, CRITICAL)
     * @param value 측정값 (숫자형)
     * @return 알림 메시지 (위험 단계에 따른 사용자 알림)
     */
    @Override
    public String getRiskMessageByWearble(WearableDataType wearableDataType, RiskLevel riskLevel, Number value) {
        return switch (wearableDataType) {
            case heartRate -> switch (riskLevel) {
                case INFO, CRITICAL,WARNING -> "심박수 %s bpm".formatted(value); // 정상 상태
            };
        };
    }
}