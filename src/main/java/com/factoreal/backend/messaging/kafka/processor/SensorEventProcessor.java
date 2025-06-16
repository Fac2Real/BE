package com.factoreal.backend.messaging.kafka.processor;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.messaging.kafka.strategy.alarmMessage.RiskMessageProvider;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.factoreal.backend.domain.state.store.ZoneSensorStateStore;
import com.factoreal.backend.domain.state.store.ZoneWorkerStateStore;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.messaging.api.AlarmEventService;
import com.factoreal.backend.messaging.api.AutoControlService;
import com.google.api.Http;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

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
    private final ZoneSensorStateStore zoneSensorStateStore;
    private final ZoneWorkerStateStore zoneWorkerStateStore;
    private final ZoneRepoService zoneRepoService;
    private final SensorRepoService sensorRepoService;
    private final RiskMessageProvider riskMessageProvider;

    /**
     * 센서 Kafka 메시지 처리
     *
     * @param dto   센서 데이터
     * @param topic Kafka 토픽명 (EQUIPMENT, ENVIRONMENT)
     */
    @Transactional
    public void process(SensorKafkaDto dto, String topic) {
        try {
            String zoneId = dto.getZoneId();
            String sensorId = dto.getSensorId();

            // 유효성 검사: zoneId와 sensorId는 필수
            if (zoneId == null || zoneId.isBlank()) {
                log.warn("⚠️ 유효하지 않은 zoneId: null 또는 빈 문자열");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 zoneId");
            } else if (sensorId == null || sensorId.isBlank()) {
                log.warn("⚠️ 유효하지 않은 sensorId: null 또는 빈 문자열");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 sensorId");
            }

            Zone zone = zoneRepoService.findById(zoneId);
            Sensor sensor = sensorRepoService.findById(sensorId);
            if (zone == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 공간 ID: " + zoneId);
            } else if (sensor == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 센서 ID: " + sensorId);
            }

            // ENVIRONMENT 토픽인 경우에만 아래 처리 로직 수행
            if ("ENVIRONMENT".equalsIgnoreCase(topic)) {
                if (dto.getEquipId() == null || !dto.getEquipId().equals(zoneId)) {
                    log.warn("⚠️ ENVIRONMENT 토픽이지만 equipId와 zoneId가 일치하지 않음: equipId={}, zoneId={}",
                            dto.getEquipId(), zoneId);
                    return;
                }

                // 위험도 계산
                int dangerLevel = dto.getDangerLevel();
                SensorType sensorType = SensorType.getSensorType(dto.getSensorType());
                RiskLevel riskLevel = RiskLevel.fromPriority(dangerLevel);
                TargetType targetType = topicToLogType(topic);

                // 이전 위험도 계산
                RiskLevel prevZoneSensorRiskLevel = zoneSensorStateStore.getZoneRiskLevel(zoneId);
                RiskLevel prevSensorRiskLevel = zoneSensorStateStore.getSensorRiskLevel(zoneId, sensorId);

                // 0. zoneSensorStateStore 업데이트
                zoneSensorStateStore.setSensorRiskLevel(zoneId, sensorId, riskLevel);

                // 현재 위험도 계산
                RiskLevel nowZoneSensorRiskLevel = zoneSensorStateStore.getZoneRiskLevel(zoneId);
                RiskLevel zoneWorkerRiskLevel = zoneWorkerStateStore.getZoneRiskLevel(zoneId);

                // WebSocket 알림 전송
                // 1. 히트맵 전송
                // 만약 존의 현재 Sensor 위험도가 Worker 위험도보다 크거나 같을 경우
                if (nowZoneSensorRiskLevel.getPriority() >= zoneWorkerRiskLevel.getPriority()) {
                    // 존의 현재 Sensor 위험도가 이전 Sensor 위험도보다 크면
                    if (nowZoneSensorRiskLevel.getPriority() >= prevZoneSensorRiskLevel.getPriority()) {
                        // 클라이언트에게 이벤트가 발생한 센서를 기반으로 sendDangerLevel 보냄
                        webSocketSender.sendDangerLevel(zoneId,
                                dto.getSensorType(), nowZoneSensorRiskLevel.getPriority());
                    }
                    // 존의 현재 Sensor 위험도가 이전 Sensor 위험도보다 작다면
                    else {
                        // 클라이언트에게 존에서 RiskLevel 높은 센서를 기반으로 sendDangerLevel 보냄
                        webSocketSender.sendDangerLevel(zoneId,
                                zoneSensorStateStore.getHighestRiskSensor(zoneId).getSensorType().name(),
                                nowZoneSensorRiskLevel.getPriority());
                    }
                }
                // 존의 현재 Sensor 위험도가 Worker 위험도보다 작고, 과거 Sensor 위험도가 Worker 위험도보다 크면
                else if (prevZoneSensorRiskLevel.getPriority() >= zoneWorkerRiskLevel.getPriority()) {
                    // 클라이언트에게 Worker 위험도를 기반으로 sendDangerLevel 보냄
                    webSocketSender.sendDangerLevel(zoneId,
                            WearableDataType.heartRate.name(), zoneWorkerRiskLevel.getPriority());
                }

                // 이벤트 Sensor에 대한 RiskLevel이 변경되면
                if (prevSensorRiskLevel.getPriority() != riskLevel.getPriority()) {
                    // abnormalLog 빌드
                    AbnormalLog abnormalLog = AbnormalLog.builder()
                            .targetId(sensorId)
                            .targetType(targetType)
                            .targetDetail(sensorType.getKoName())
                            .abnormalType(riskMessageProvider.getRiskMessageBySensor(sensorType, riskLevel, dto.getVal()))
                            .abnVal(dto.getVal())
                            .dangerLevel(riskLevel.getPriority())
                            .zone(zone)
                            .detectedAt(LocalDateTime.parse(dto.getTime()))
                            .isRead(false)
                            .build();
                    // 2-1. 이상 로그 저장
                    AbnormalLog abnLog = abnormalLogService.saveAbnormalLog(abnormalLog);

                    // 2-2. 위험 알림 전송 -> 위험도별 Websocket + wearable + Slack(SMS 대체)
                    // Todo : (As-is) 전략 기반 startAlarm() 메서드 담당자 확인 필요 -> 확인됨
                    alarmEventService.startAlarm(dto, abnLog, dangerLevel);



                    // 2-3. 자동 제어 메시지 판단
                    try {
                        autoControlService.evaluate(dto, abnLog, dangerLevel);
                    } catch (Exception e) {
                        log.info("자동 제어 기능은 제작중인 기능입니다. Todo 입니다.");
                    }
                }

                // 3. 읽지 않은 수 전송
                Long count = abnormalLogRepoService.countByIsReadFalse();
                webSocketSender.sendUnreadCount(count);

                log.info("✅ 센서 이벤트 처리 완료: sensorId={}, zoneId={}, level={} ({} topic)",
                        sensorId, zoneId, dangerLevel, topic);
            }
        } catch (Exception e) {
            log.error("❌ 센서 이벤트 처리 실패: sensorId={}, zoneId={}", dto.getSensorId(), dto.getZoneId(), e);
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
