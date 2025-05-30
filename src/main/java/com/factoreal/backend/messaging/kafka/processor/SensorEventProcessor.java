package com.factoreal.backend.messaging.kafka.processor;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.stateStore.InMemoryZoneSensorStateStore;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.messaging.service.AlarmEventService;
import com.factoreal.backend.messaging.service.AutoControlService;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SensorEventProcessor 클래스는 Kafka로부터 전달받은 센서 데이터를 분석 및 처리하는 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorEventProcessor {

    private final AutoControlService autoControlService;
    private final AbnormalLogService abnormalLogService;
    private final AbnormalLogRepoService abnormalLogRepoService;
    private final AlarmEventService alarmEventService;
    private final WebSocketSender webSocketSender;
    private final InMemoryZoneSensorStateStore zoneSensorStateStore;

    /**
     * 센서 Kafka 메시지 처리
     *
     * @param dto   센서 데이터
     * @param topic Kafka 토픽명 (EQUIPMENT, ENVIRONMENT)
     */
    public void process(SensorKafkaDto dto, String topic) {
        try {

            // 유효성 검사: zoneId와 sensorId는 필수
            if (dto.getZoneId() == null || dto.getZoneId().isBlank()) {
                log.warn("⚠️ 유효하지 않은 zoneId: null 또는 빈 문자열");
                return;
            }
            if (dto.getSensorId() == null || dto.getSensorId().isBlank()) {
                log.warn("⚠️ 유효하지 않은 sensorId: null 또는 빈 문자열");
                return;
            }

            // ENVIRONMENT 토픽인 경우에만 아래 처리 로직 수행
            if ("ENVIRONMENT".equalsIgnoreCase(topic)) {
                if (dto.getEquipId() == null || !dto.getEquipId().equals(dto.getZoneId())) {
                    log.warn("⚠️ ENVIRONMENT 토픽이지만 equipId와 zoneId가 일치하지 않음: equipId={}, zoneId={}",
                            dto.getEquipId(), dto.getZoneId());
                    return;
                }

                // 위험도 계산
                int dangerLevel = dto.getDangerLevel();
                SensorType sensorType = SensorType.getSensorType(dto.getSensorType());
                RiskLevel riskLevel = RiskLevel.fromPriority(dangerLevel);
                TargetType targetType = topicToLogType(topic);

                try {
                    // 자동 제어 메시지 판단 (Todo - 진행중)
                    autoControlService.evaluate(dto, dangerLevel);
                }catch(Exception e){
                    log.info("자동 제어 기능은 제작중인 기능입니다. Todo 입니다.");
                }

                // WebSocket 알림 전송
                // 1. 히트맵 전송
                webSocketSender.sendDangerLevel(dto.getZoneId(), dto.getSensorType(), dangerLevel);

                RiskLevel prevSensorRiskLevel = zoneSensorStateStore.getSensorRiskLevel(dto.getZoneId(), dto.getSensorId());
                if (prevSensorRiskLevel.getPriority() != riskLevel.getPriority()) {
                    // 2-1. state 업데이트
                    zoneSensorStateStore.setSensorRiskLevel(dto.getZoneId(), dto.getSensorId(), riskLevel);

                    // 2-2. 이상 로그 저장
                    AbnormalLog abnLog = abnormalLogService.saveAbnormalLogFromSensorKafkaDto(
                            dto, sensorType, riskLevel, targetType
                    );

                    // 2-3. 위험 알림 전송 -> 위험도별 Websocket + wearable + Slack(SMS 대체)
                    // Todo : (As-is) 전략 기반 startAlarm() 메서드 담당자 확인 필요
                    // webSocketSender.sendDangerAlarm(abnLog.toAlarmEventDto());
                    alarmEventService.startAlarm(dto, abnLog, dangerLevel);
                }

                // 3. 읽지 않은 수 전송
                Long count = abnormalLogRepoService.countByIsReadFalse();
                webSocketSender.sendUnreadCount(count);

                log.info("✅ 센서 이벤트 처리 완료: sensorId={}, zoneId={}, level={} ({} topic)",
                        dto.getSensorId(), dto.getZoneId(), dangerLevel, topic);
            }
        } catch (Exception e) {
            log.error("❌ 센서 이벤트 처리 실패: sensorId={}, zoneId={}", dto.getSensorId(), dto.getZoneId(), e);
        }
    }

    // 공간에 위험도 분기 로직
    // Flink에서 적용으로 변경되어 사용안함
    @Deprecated
    private int getDangerLevel(String sensorType, double val) {
        switch (sensorType.toLowerCase()) {
            case "temp":
                return val > 40 || val < -35 ? 2 : (val > 30 || val < 25 ? 1 : 0);
            case "humid":
                return val >= 80 ? 2 : (val >= 60 ? 1 : 0);
            case "vibration":
                return val > 7.1 ? 2 : (val > 2.8 ? 1 : 0);
            case "current":
                return val >= 30 ? 2 : (val >= 7 ? 1 : 0);
            case "dust":
                return val >= 150 ? 2 : (val >= 75 ? 1 : 0);
            case "voc":
                return val >= 1000 ? 2 : (val >= 300 ? 1 : 0);
            default:
                return 0;
        }
    }

    // topic enum 변경하기
    private TargetType topicToLogType(String topic) {
        return switch (topic.toUpperCase()) {
            case "EQUIPMENT" -> TargetType.Equip;
            case "ENVIRONMENT" -> TargetType.Sensor;
            default -> throw new IllegalArgumentException("지원하지 않는 Kafka 토픽: " + topic);
        };
    }
}
