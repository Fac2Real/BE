package com.factoreal.backend.messaging.api;

import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.messaging.kafka.dto.WearableKafkaDto;
import com.factoreal.backend.messaging.kafka.strategy.NotificationStrategyFactory;
import com.factoreal.backend.messaging.kafka.strategy.alarmList.NotificationStrategy;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventResponse;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlarmEventService {

    // 위험 레벨별 알람 전략을 가져오기 위한 팩토리 서비스
    private final NotificationStrategyFactory notificationStrategyFactory;
    private final ZoneRepoService zoneRepoService;

    // Todo 추후 Flink에서 SensorKafkaDto에 dangerLevel을 포함하면 제거
    public void startAlarm(SensorKafkaDto sensorData, AbnormalLog abnormalLog, int dangerLevel) {
        AlarmEventResponse alarmEventResponse;
        RiskLevel riskLevel = RiskLevel.fromPriority(dangerLevel);

        try {
            // 1. dangerLevel기준으로 alarmEvent 객체 생성.
            alarmEventResponse = generateAlarmDto(sensorData, abnormalLog, riskLevel);
        } catch (Exception e) {
            log.error("Error converting Kafka message: {}", e);
            return;
        }
        // 1-1. AbnormalLog 기록.
        try {
            // 2. 생성된 AlarmEvent DTO 객체를 사용하여 알람 처리
            log.info("alarmEvent: {}", alarmEventResponse.toString());
            processAlarmEvent(alarmEventResponse);
        } catch (Exception e) {
            log.error("Error converting Kafka message: {}", e.getMessage());
            // TODO: 기타 처리 오류 처리
        }
    }

    public AlarmEventResponse generateAlarmDto(WearableKafkaDto data, AbnormalLog abnormalLog, RiskLevel riskLevel) {
        String source = "웨어러블";

        // 알람 이벤트 객체 반환
        return AlarmEventResponse.builder()
                .eventId(abnormalLog.getId())
                .sensorId(data.getWearableDeviceId()) // 웨어러블 디바이스 Id
                .equipId(abnormalLog.getZone().getZoneId())
                .zoneId(abnormalLog.getZone().getZoneId())
                .sensorType(WearableDataType.heartRate.toString())
                .messageBody(abnormalLog.getAbnormalType()) // 이상에 대한 메세지
                .source(source)
                .time(data.getTime())
                .riskLevel(riskLevel)
                .zoneName(abnormalLog.getZone().getZoneName())
                .build();
    }

    protected AlarmEventResponse generateAlarmDto(SensorKafkaDto data, AbnormalLog abnormalLog, RiskLevel riskLevel)
            throws Exception {

        String source = data.getZoneId().equals(data.getEquipId()) ? "공간 센서" : "설비 센서";
        SensorType sensorType = SensorType.valueOf(data.getSensorType());
        String zoneName = zoneRepoService.findById(data.getZoneId()).getZoneName();
        // 알람 이벤트 객체 반환
        return AlarmEventResponse.builder()
                .eventId(abnormalLog.getId())
                .sensorId(data.getSensorId())
                .equipId(data.getEquipId())
                .zoneId(data.getZoneId())
                .sensorType(sensorType.name())
                .messageBody(abnormalLog.getAbnormalType())
                .source(source)
                .time(data.getTime())
                .riskLevel(riskLevel)
                .zoneName(zoneName)
                .build();
    }

    protected void processAlarmEvent(AlarmEventResponse alarmEventResponse) {
        if (alarmEventResponse == null || alarmEventResponse.getRiskLevel() == null) {
            log.warn("Received null AlarmEvent DTO or DTO with null severity. Skipping notification.");
            return;
        }

        try {
            log.info("Processing AlarmEvent with mapped Entity RiskLevel: {}", alarmEventResponse.getRiskLevel());

            // 3. Factory를 사용하여 매핑된 Entity RiskLevel에 해당하는 NotificationStrategy를 가져와 실행
            List<NotificationStrategy> notificationStrategyList =
                    notificationStrategyFactory.getStrategiesForLevel(alarmEventResponse.getRiskLevel());

            log.info("💡Notification strategy executed for AlarmEvent. \n{}", alarmEventResponse.toString());
            // 4. 알람 객체의 값으로 전략별 알람 송신.
            notificationStrategyList.forEach(notificationStrategy -> notificationStrategy.send(alarmEventResponse));

        } catch (Exception e) {
            log.error("Failed to execute notification strategy for AlarmEvent DTO: {}", alarmEventResponse, e);
            // TODO: 전략 실행 중 오류 처리
        }
    }
}
