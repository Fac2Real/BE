package com.factoreal.backend.messaging.kafka;

import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.zone.dao.ZoneRepository;
import com.factoreal.backend.messaging.common.dto.SystemLogDto;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.domain.zone.application.ZoneService;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.messaging.kafka.strategy.alarmList.NotificationStrategy;
import com.factoreal.backend.messaging.kafka.strategy.NotificationStrategyFactory;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.factoreal.backend.domain.sensor.application.SensorService;
import com.factoreal.backend.domain.sensor.entity.Sensor;

import java.time.ZonedDateTime;
import java.time.ZoneId;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerD {

    private final ObjectMapper objectMapper;
    private final WebSocketSender webSocketSender;
    private final ZoneRepository zoneRepository;
    private final ZoneService zoneService;

    // 알람 푸시 용
    private final NotificationStrategyFactory factory;

    // 공간(zone)별로 마지막 위험도 저장하기 위한 Map (초기에는 위험도 -1)
    private static final Map<String, Integer> lastDangerLevelMap = new ConcurrentHashMap<>();

    // 로그 기록용
    private final AbnormalLogService abnormalLogService;

    private final SensorService sensorService;

    // @KafkaListener(topics = {"EQUIPMENT", "ENVIRONMENT"}, groupId =
    // "monitory-consumer-group-1")
//    @KafkaListener(topics = { "EQUIPMENT",
//            "ENVIRONMENT" }, groupId = "${spring.kafka.consumer.group-id:danger-alert-group}")
    public void consume(String message) {

        log.info("💡수신한 Kafka 메시지 : " + message);
        try {
            SensorKafkaDto dto = objectMapper.readValue(message, SensorKafkaDto.class);

            // 시스템 로그 (위험도 변화 감지 -> 비동기 전송)
            // sendSystemLog(dto);

            // 공간 센서일 때만 히트맵용 웹소켓 전송
            if (dto.getEquipId() != null && dto.getZoneId() != null && dto.getEquipId().equals(dto.getZoneId())) {
                log.info("✅ 공간 센서 로직 start");

                log.info("▶︎ 위험도 감지 start");
                int dangerLevel = getDangerLevel(dto.getSensorType(), dto.getVal());
                log.info("⚠️ 위험도 {} 센서 타입 : {} 감지됨. Zone: {}", dangerLevel, dto.getSensorType(), dto.getZoneId());

                // 자동제어 로직: threshold 및 오차범위 벗어나면 메시지 전송
                // 중첩 try-catch 문 : Kafka 메시지 처리에서 자동제어 로직은 실패해도, 전체 처리는 멈추지 않게 하기 위해
                performAutoControl(dto);

                // #################################
                // Abnormal 로그 기록 로직
                // #################################
                SensorType sensorType = SensorType.getSensorType(dto.getSensorType());
                RiskLevel riskLevel = RiskLevel.fromPriority(dangerLevel);
                if (sensorType == null) {
                    log.error("SensorType not found");
                    throw new Exception("SensorType not found");
                }
                AbnormalLog abnormalLog = abnormalLogService.saveAbnormalLogFromSensorKafkaDto(
                        dto,
                        sensorType,
                        riskLevel,
                        TargetType.Sensor);

                // #################################
                // 웹 앱 SMS 알람 로직
                // #################################
                startAlarm(dto, abnormalLog, riskLevel);

                // #################################
                // 대시보드용 히트맵 로직
                // #################################
                // ❗dangerLevel이 0일 때도 전송해야되면 if 문은 필요없을 것 같아 제거.

                webSocketSender.sendDangerLevel(dto.getZoneId(), dto.getSensorType(), dangerLevel);
                abnormalLogService.readRequired(); // 읽지 않은 알람 수
            }

        } catch (Exception e) {
            log.error("❌ Kafka 메시지 파싱 실패: {}", message, e);
        }

    }

    @Async
    public void startAlarm(SensorKafkaDto sensorData, AbnormalLog abnormalLog, RiskLevel riskLevel) {
        AlarmEventDto alarmEventDto;
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
            processAlarmEvent(alarmEventDto, riskLevel);
        } catch (Exception e) {
            log.error("Error converting Kafka message: {}", e);
            // TODO: 기타 처리 오류 처리
        }
    }

    // 공간(zone)별 위험도 변경 시 시스템 로그 전송
    @Async
    @Deprecated
    public void sendSystemLog(SensorKafkaDto dto) {
        String zoneId = dto.getZoneId();
        int newLevel = getDangerLevel(dto.getSensorType(), dto.getVal());
        int oldLevel = lastDangerLevelMap.getOrDefault(zoneId, -1);

        // 변경이 없으면 로그 전송 안함
        if (newLevel == oldLevel) {
            lastDangerLevelMap.put(zoneId, newLevel);
            return;
        }
        lastDangerLevelMap.put(zoneId, newLevel); // 변경이 있으니 해당 공간의 마지막 위험도 업데이트

        // zoneName 조회
        String zoneName = zoneService.getAllZones().stream()
                .filter(zone -> zone.getZoneId().equals(zoneId))
                .findFirst()
                .map(zone -> zone.getZoneName())
                .orElse("");

        // ISO-8601 포맷 타임스탬프 ex) 2025-05-09T16:22:45
        // String timestamp = LocalDateTime.now()
        //         .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String timestamp = ZonedDateTime
                .now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        SystemLogDto logDto = new SystemLogDto(
                zoneId, zoneName,
                dto.getSensorType(),
                newLevel,
                dto.getVal(),       // 이 부분 추가
                timestamp);

        webSocketSender.sendSystemLog(logDto);
    }

    private static int getDangerLevel(String sensorType, double value) { // 위험도 계산 메서드
        return switch (sensorType) { // 센서 타입에 따른 위험도 계산
            case "temp" -> { // 온도 위험도 기준 (KOSHA: https://www.kosha.or.kr/)
                if (value > 40 || value < -35) // >40℃ 또는 < -35℃ → 위험 (작업 중단 권고)
                    yield 2;
                else if (value > 30 || value < 25) // >30℃ 또는 < 25℃ → 주의 (작업 제한 또는 휴식 권고)
                    yield 1;
                else // 25℃ ≤ value ≤ 30℃ → 안전 (권장 18~21℃)
                    yield 0;
            }

            case "humid" -> { // 상대습도 위험도 기준 (OSHA, ACGIH TLV®, NIOSH)
                if (value >= 80) // RH ≥ 80% → 위험
                    yield 2;
                else if (value >= 60) // 60% ≤ RH < 80% → 주의
                    yield 1;
                else // RH < 60% → 안전
                    yield 0;
            }

            case "vibration" -> { // 진동 위험도 기준 (ISO 10816-3)
                if (value > 7.1) // >7.1 mm/s → 위험
                    yield 2;
                else if (value > 2.8) // >2.8 mm/s → 주의
                    yield 1;
                else // ≤2.8 mm/s → 안전
                    yield 0;
            }

            case "current" -> { // 전류 위험도 기준 (KEPCO)
                if (value >= 30) // ≥30 mA → 위험 (강한 경련, 심실세동 및 사망 위험)
                    yield 2;
                else if (value >= 7) // ≥7 mA → 주의 (고통 한계 전류, 불수전류)
                    yield 1;
                else // <7 mA → 안전 (감지전류 수준)
                    yield 0;
            }

            case "dust" -> { // PM2.5 위험도 기준 (고용노동부)
                if (value >= 150) // ≥ 150㎍/㎥ → 위험
                    yield 2;
                else if (value >= 75) // ≥ 75㎍/㎥ → 주의
                    yield 1;
                else // < 75㎍/㎥ → 안전
                    yield 0;
            }

            // 그 외 센서 타입은 안전
            default -> 0;
        };
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

    private void processAlarmEvent(AlarmEventDto alarmEventDto, RiskLevel riskLevel) {
        if (alarmEventDto == null || alarmEventDto.getRiskLevel() == null) {
            log.warn("Received null AlarmEvent DTO or DTO with null severity. Skipping notification.");
            return;
        }

        try {
            if (riskLevel == null) {
                log.warn("Could not map DTO severity '{}' to Entity RiskLevel. Skipping notification.",
                        alarmEventDto.getRiskLevel());

                // TODO: 매핑 실패 시 처리 로직 추가
                return;
            }

            log.info("Processing AlarmEvent with mapped Entity RiskLevel: {}", riskLevel);

            // 3. Factory를 사용하여 매핑된 Entity RiskLevel에 해당하는 NotificationStrategy를 가져와 실행
            List<NotificationStrategy> notificationStrategyList = factory.getStrategiesForLevel(riskLevel);

            log.info("💡Notification strategy executed for AlarmEvent. \n{}", alarmEventDto.toString());
            // 4. 알람 객체의 값으로 전략별 알람 송신.
            notificationStrategyList.forEach(notificationStrategy -> notificationStrategy.send(alarmEventDto));

        } catch (Exception e) {
            log.error("Failed to execute notification strategy for AlarmEvent DTO: {}", alarmEventDto, e);
            // TODO: 전략 실행 중 오류 처리
        }
    }

    /**
     * 측정값이 (threshold ± allowVal) 범위를 벗어나면 제어 메시지 생성
     */
    private void performAutoControl(SensorKafkaDto dto) {
        try {
            Sensor sensor = sensorService.getSensorById(dto.getSensorId());
            String type = sensor.getSensorType().name();
            double threshold = sensor.getSensorThres();
            double tolerance = sensor.getAllowVal() != null ? sensor.getAllowVal() : 0.0;
            double value = dto.getVal();

            if (value < threshold - tolerance || value > threshold + tolerance) {
                String msg = buildControlMessage(type, value, threshold, tolerance);
                log.info("[자동제어 메시지] {}", msg);
                // TODO: MQTT 퍼블리시 로직으로 대체
            }
        } catch (Exception e) {
            log.error("자동제어 오류 처리 중 예외 발생", e);
        }
    }

    private String buildControlMessage(  // 제어 로직
                                         String type, double val, double thresh, double tol) {
        return switch (type.toLowerCase()) {
            case "temp" -> String.format("현재 온도는 %.1f℃입니다. 적정 온도 범위는 %.1f~%.1f℃입니다.",
                    val, thresh - tol, thresh + tol);
            case "humid" -> String.format("현재 습도는 %.1f%%입니다. 적정 습도 범위는 %.1f~%.1f%%입니다.",
                    val, thresh - tol, thresh + tol);
            case "vibration" -> String.format("현재 진동 값은 %.1fmm/s입니다. 허용 범위는 %.1f~%.1fmm/s입니다.",
                    val, thresh - tol, thresh + tol);
            case "current" -> String.format("현재 전류는 %.1fmA입니다. 허용 범위는 %.1f~%.1fmA입니다.",
                    val, thresh - tol, thresh + tol);
            case "dust" -> String.format("현재 미세먼지는 %.1f㎍/㎥입니다. 허용 범위는 %.1f~%.1f㎍/㎥입니다.",
                    val, thresh - tol, thresh + tol);
            default -> String.format("현재 값은 %.1f이고, 허용 범위는 %.1f~%.1f입니다.",
                    val, thresh - tol, thresh + tol);
        };

    }
}