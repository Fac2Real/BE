package com.factoreal.backend.messaging.service;

import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.zone.dao.ZoneRepository;
import com.factoreal.backend.messaging.kafka.strategy.alarmList.NotificationStrategy;
import com.factoreal.backend.messaging.kafka.strategy.NotificationStrategyFactory;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
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
    private final ZoneRepository zoneRepository;
    // Todo 추후 Flink에서 SensorKafkaDto에 dangerLevel을 포함하면 제거
    public void startAlarm(SensorKafkaDto sensorData, AbnormalLog abnormalLog, int dangerLevel) {
        AlarmEventDto alarmEventDto;
        RiskLevel riskLevel = RiskLevel.fromPriority(dangerLevel);
    //  Todo Flink 수정시 주석 해제
    //  RiskLevel riskLevel = RiskLevel.fromPriority(sensorData.getDangerLevel);

        try {
            // 1. dangerLevel기준으로 alarmEvent 객체 생성.
            alarmEventDto = generateAlarmDto(sensorData, abnormalLog, riskLevel);
        } catch (Exception e) {
            log.error("Error converting Kafka message: {}", e);
            return;
        }
        // 1-1. AbnormalLog 기록.
        try {
            // 2. 생성된 AlarmEvent DTO 객체를 사용하여 알람 처리
            log.info("alarmEvent: {}", alarmEventDto.toString());
            processAlarmEvent(alarmEventDto);
        } catch (Exception e) {
            log.error("Error converting Kafka message: {}", e);
            // TODO: 기타 처리 오류 처리
        }
    }
    private AlarmEventDto generateAlarmDto(SensorKafkaDto data, AbnormalLog abnormalLog, RiskLevel riskLevel)
            throws Exception {

        String source = data.getZoneId().equals(data.getEquipId()) ? "공간 센서" : "설비 센서";
        SensorType sensorType = SensorType.valueOf(data.getSensorType());
        String zoneName = zoneRepository.findByZoneId(data.getZoneId()).getZoneName();
        // 알람 이벤트 객체 반환
        return AlarmEventDto.builder()
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
    private void processAlarmEvent(AlarmEventDto alarmEventDto) {
        if (alarmEventDto == null || alarmEventDto.getRiskLevel() == null) {
            log.warn("Received null AlarmEvent DTO or DTO with null severity. Skipping notification.");
            return;
        }

        try {
            log.info("Processing AlarmEvent with mapped Entity RiskLevel: {}", alarmEventDto.getRiskLevel());

            // 3. Factory를 사용하여 매핑된 Entity RiskLevel에 해당하는 NotificationStrategy를 가져와 실행
            List<NotificationStrategy> notificationStrategyList =
                    notificationStrategyFactory.getStrategiesForLevel(alarmEventDto.getRiskLevel());

            log.info("💡Notification strategy executed for AlarmEvent. \n{}", alarmEventDto.toString());
            // 4. 알람 객체의 값으로 전략별 알람 송신.
            notificationStrategyList.forEach(notificationStrategy -> notificationStrategy.send(alarmEventDto));

        } catch (Exception e) {
            log.error("Failed to execute notification strategy for AlarmEvent DTO: {}", alarmEventDto, e);
            // TODO: 전략 실행 중 오류 처리
        }
    }
}
