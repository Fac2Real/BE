package com.factoreal.backend.messaging.kafka.processor;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.state.store.ZoneSensorStateStore;
import com.factoreal.backend.domain.state.store.ZoneWorkerStateStore;
import com.factoreal.backend.domain.zone.application.ZoneHistoryRepoService;
import com.factoreal.backend.domain.zone.application.ZoneHistoryService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.messaging.kafka.dto.WearableKafkaDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventResponse;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.messaging.api.AlarmEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;


/**
 * WearableEventProcessor 클래스는 Kafka로부터 전달받은 생체 데이터를 처리하는 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WearableEventProcessor {
    private final AbnormalLogService abnormalLogService;
    private final AbnormalLogRepoService abnormalLogRepoService;
    private final WebSocketSender webSocketSender;
    private final AlarmEventService alarmEventService;
    private final ZoneWorkerStateStore zoneWorkerStateStore;
    private final ZoneHistoryService zoneHistoryService;
    private final ZoneHistoryRepoService zoneHistoryRepoService;
    private final ZoneSensorStateStore zoneSensorStateStore;
    private static final String DEFAULT_ZONE_ID = "00000000000000-000";

    /**
     * kafka 메시지 처리
     * topic은 고정값 하나만 받으므로 생략함.
     *
     * @param dto   생체 데이터
     * @param topic Kafka 토픽명(WEARABLE)
     */
    public void process(WearableKafkaDto dto, String topic) {
        try {
            // 위험도는 0:정상, 2:비정상으로 나뉨
            // wearable자체에서 rule-based 기반으로 할당되어 송신됨.
            int dangerLevel = dto.getDangerLevel(); // 0: 정상, 2: 비정상

            WearableDataType wearableDataType = WearableDataType.getWearableType(dto.getSensorType());
            RiskLevel riskLevel = RiskLevel.fromPriority(dangerLevel);
            String workerId = dto.getWorkerId();
            String zoneId = Optional.ofNullable(zoneWorkerStateStore.getZoneId(workerId))
                    .or(() ->
                            Optional.ofNullable(zoneHistoryRepoService.getCurrentWorkerLocation(workerId))
                                    .map(ZoneHist::getZone)
                                    .map(Zone::getZoneId)
                    ).orElseGet(() -> {
                        zoneHistoryService.updateWorkerLocation(workerId, DEFAULT_ZONE_ID, LocalDateTime.now());
                        return zoneWorkerStateStore.getZoneId(workerId);
                    });

            // 이전 위험도 계산
            RiskLevel prevZoneWorkerRiskLevel = zoneWorkerStateStore.getZoneRiskLevel(zoneId);
            RiskLevel prevWorkerRiskLevel = zoneWorkerStateStore.getWorkerRiskLevel(workerId);

            // 0. zoneWorkerStateStore 업데이트
            zoneWorkerStateStore.setWorkerRiskLevel(zoneId, workerId, riskLevel);

            // 현재 위험도 계산
            RiskLevel nowZoneWorkerRiskLevel = zoneWorkerStateStore.getZoneRiskLevel(zoneId);
            RiskLevel zoneSensorRiskLevel = zoneSensorStateStore.getZoneRiskLevel(zoneId);

            // 타겟타입이 항상 WEARABLE이므로 TargetType.Worker 바로 사용
            // WebSocket 알림 전송
            // 1. 히트맵 전송
            // 만약 존의 현재 Worker 위험도가 Sensor 위험도보다 크거나 같을 경우
            if (nowZoneWorkerRiskLevel.getPriority() >= zoneSensorRiskLevel.getPriority()) {
                // 존의 현재 Worker 위험도가 이전 Worker 위험도보다 크면
                if (nowZoneWorkerRiskLevel.getPriority() >= prevZoneWorkerRiskLevel.getPriority()) {
                    // 클라이언트에게 이벤트가 발생한 Worker 기반으로 sendDangerLevel 보냄
                    webSocketSender.sendDangerLevel(zoneId, WearableDataType.heartRate.name(), nowZoneWorkerRiskLevel.getPriority());
                }
                // 존의 현재 Worker 위험도가 이전 Worker 위험도보다 작다면
                else {
                    // 클라이언트에게 존에서 RiskLevel 높은 Worker 기반으로 sendDangerLevel 보냄 <TODO: WearableDataType 추가시 변경 요망>
                    webSocketSender.sendDangerLevel(zoneId, WearableDataType.heartRate.name(), nowZoneWorkerRiskLevel.getPriority());
                }
            }
            // 존의 현재 Worker 위험도가 Sensor 위험도보다 작고, 과거 Worker 위험도가 Sensor 위험도보다 크면
            else if (prevZoneWorkerRiskLevel.getPriority() >= zoneSensorRiskLevel.getPriority()) {
                // 클라이언트에게 Sensor 위험도를 기반으로 sendDangerLevel 보냄
                webSocketSender.sendDangerLevel(zoneId,
                        zoneSensorStateStore.getHighestRiskSensor(zoneId).getSensorType().name(),
                        zoneSensorRiskLevel.getPriority());
            }

            // 이벤트 Worker에 대한 RiskLevel이 변경되면
            if (prevWorkerRiskLevel.getPriority() != riskLevel.getPriority()) {
                // 2-1. abnormalLog 기록
                AbnormalLog abnormalLog = abnormalLogService.saveAbnormalLogFromWearableKafkaDto(
                        dto, wearableDataType, riskLevel, TargetType.Worker
                );

                // 2-2. 상세 화면으로 웹소켓 보내는 것을 생략
                // 3. 위험 알림 전송 -> 팝업으로 알려주기
                AlarmEventResponse alarmEventResponse = alarmEventService.generateAlarmDto(dto, abnormalLog, riskLevel);
                webSocketSender.sendDangerAlarm(alarmEventResponse);
            }

            // 4. 읽지 않은 알림 전송
            Long count = abnormalLogRepoService.countByIsReadFalse();
            webSocketSender.sendUnreadCount(count);

        } catch (Exception e) {
            log.error(
                    "❌ 웨어러블 이벤트 처리 실패: sensorId={}, zoneId={}",
                    dto.getWearableDeviceId(),
                    dto.getWorkerId()
            );
        }
    }
}
