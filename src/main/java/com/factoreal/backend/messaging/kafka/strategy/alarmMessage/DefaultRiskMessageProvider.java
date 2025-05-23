package com.factoreal.backend.messaging.kafka.strategy.alarmMessage;

import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import org.springframework.stereotype.Component;

@Component
public class DefaultRiskMessageProvider implements RiskMessageProvider {
    @Override
    public String getMessage(SensorType sensorType, RiskLevel riskLevel) {
        return switch (sensorType) {
            case temp -> switch (riskLevel) {
                case INFO -> "온도가 정상 범위입니다.";
                case WARNING -> "경고! 온도가 30도를 초과했습니다.";
                case CRITICAL -> "위험! 온도가 50도를 초과했습니다.";
            };
            case humid -> switch (riskLevel) {
                case INFO -> "습도가 정상 범위입니다.";
                case WARNING -> "경고! 습도가 50%을 초과했습니다.";
                case CRITICAL -> "위험! 습도가 70%을 초과했습니다.";
            };
            case vibration -> switch (riskLevel) {
                case INFO -> "진동이 정상 범위입니다.";
                case WARNING -> "주의! 진동이 5 이상으로 감지됨.";
                case CRITICAL -> "위험! 진동이 10 이상으로 매우 심합니다.";
            };
            case dust -> switch (riskLevel) {
                case INFO -> "먼지 농도 정상.";
                case WARNING -> "주의! 먼지 농도가 다소 높습니다.";
                case CRITICAL -> "위험! 먼지 농도가 40 이상입니다.";
            };
            case voc -> switch (riskLevel) {
                case INFO -> "VOC 농도 정상.";
                case WARNING -> "VOC 경고! 공기 중 오염 물질이 감지됨.";
                case CRITICAL -> "위험! VOC 농도가 높습니다. 환기 필요.";
            };
            case current -> switch (riskLevel) {
                case INFO -> "전류 정상.";
                case WARNING -> "과전류 주의! 5A 초과 감지.";
                case CRITICAL -> "위험! 과전류가 7A를 초과했습니다.";
            };
        };
    }
}
