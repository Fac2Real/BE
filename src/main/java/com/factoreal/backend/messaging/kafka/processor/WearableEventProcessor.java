package com.factoreal.backend.messaging.kafka.processor;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.messaging.kafka.dto.WearableKafkaDto;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.messaging.service.AlarmEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * WearableEventProcessor 클래스는 Kafka로부터 전달받은 생체 데이터를 처리하는 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WearableEventProcessor {
    private final AbnormalLogService abnormalLogService;
    private final WebSocketSender webSocketSender;
    private final AlarmEventService alarmEventService;

    /**
     * kafka 메시지 처리
     * topic은 고정값 하나만 받으므로 생략함.
     * @param wearableKafkaDto 생체 데이터
     * @param topic Kafka 토픽명(WEARABLE)
     */
    public void process(WearableKafkaDto wearableKafkaDto, String topic) {
        try{
            // 위험도는 0:정상, 1:비정상으로 나뉨
            // wearable자체에서 rule-based 기반으로 할당되어 송신됨.
            int dangerLevel = wearableKafkaDto.getDangerLevel(); // 0: 정상, 1: 비정상

            WearableDataType wearableDataType = WearableDataType.getWearableType(wearableKafkaDto.getSensorType());
            RiskLevel riskLevel = RiskLevel.fromPriority(dangerLevel);

            // 타겟타입이 항상 WEARABLE이므로 TargetType.Worker 바로 사용
            // 1. abnormalLog 기록
            AbnormalLog abnormalLog = abnormalLogService.saveAbnormalLogFromWearableKafkaDto(
                    wearableKafkaDto, wearableDataType, riskLevel, TargetType.Worker
            );

            // 읽지 않은 알림 수 조회
            Long count = abnormalLogService.readRequired();

            // WebSocket 알림 전송
            // 1. 히트맵 전송
            webSocketSender.sendDangerLevel(
                    abnormalLog.getZone().getZoneId(),
                    WearableDataType.heartRate.name(),
                    dangerLevel
            );
            // 2. 상세 화면으로 웹소켓 보내는 것을 생략
            // 3. 위험 알림 전송 -> 팝업으로 알려주기
            AlarmEventDto alarmEventDto = alarmEventService.generateAlarmDto(wearableKafkaDto,abnormalLog,riskLevel);
            webSocketSender.sendDangerAlarm(alarmEventDto);
            // 4. 읽지 않은 알림 전송
            webSocketSender.sendUnreadCount(count);

        }catch (Exception e){
            log.error(
                    "❌ 웨어러블 이벤트 처리 실패: sensorId={}, zoneId={}",
                    wearableKafkaDto.getWearableDeviceId(),
                    wearableKafkaDto.getWorkerId()
            );
        }
    }
}
