package com.factoreal.backend.messaging.kafka.processor;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.notifyLog.service.NotifyLogService;
import com.factoreal.backend.domain.stateStore.InMemoryZoneWorkerStateStore;
import com.factoreal.backend.domain.zone.application.ZoneHistoryRepoService;
import com.factoreal.backend.domain.zone.application.ZoneHistoryService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.messaging.kafka.dto.WearableKafkaDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.messaging.service.AlarmEventService;
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
    private final InMemoryZoneWorkerStateStore zoneWorkerStateStore;
    private final ZoneHistoryService zoneHistoryService;
    private final ZoneHistoryRepoService zoneHistoryRepoService;
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

            // 타겟타입이 항상 WEARABLE이므로 TargetType.Worker 바로 사용
            // WebSocket 알림 전송
            // 1. 히트맵 전송
            webSocketSender.sendDangerLevel(
                    zoneId,
                    WearableDataType.heartRate.name(),
                    dangerLevel
            );

            RiskLevel prevWorkerRiskLevel = zoneWorkerStateStore.getWorkerRiskLevel(workerId);
            if (prevWorkerRiskLevel.getPriority() != riskLevel.getPriority()) {
                // 2-1. state 업데이트
                zoneWorkerStateStore.setWorkerRiskLevel(zoneId, workerId, riskLevel);

                // 2-2. abnormalLog 기록
                AbnormalLog abnormalLog = abnormalLogService.saveAbnormalLogFromWearableKafkaDto(
                        dto, wearableDataType, riskLevel, TargetType.Worker
                );

                // 2-3. 상세 화면으로 웹소켓 보내는 것을 생략
                // 3. 위험 알림 전송 -> 팝업으로 알려주기
                AlarmEventDto alarmEventDto = alarmEventService.generateAlarmDto(dto, abnormalLog, riskLevel);
                webSocketSender.sendDangerAlarm(alarmEventDto);
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
