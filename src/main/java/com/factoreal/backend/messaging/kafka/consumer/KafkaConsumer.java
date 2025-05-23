package com.factoreal.backend.messaging.kafka.consumer;

import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.messaging.kafka.processor.SensorEventProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


/**
 * KafkaConsumer 클래스는 Kafka 토픽으로부터 메시지를 수신하고,
 * 해당 메시지를 SensorEventProcessor.java 전달
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final ObjectMapper objectMapper;
    private final SensorEventProcessor sensorEventProcessor;

    // 설비 센서 관련 Kafka 메시지 처리
    // Todo : 설비 머신러닝 끝나고 수정 예정
//    @KafkaListener(topics = "EQUIPMENT", groupId = "equipment-consumer-group")
    public void consumeEquipment(String message) {
        log.info("📩 [EQUIPMENT] Kafka 메시지 수신: {}", message);
        handleMessage(message, "EQUIPMENT");
    }

    // 공간 센서 관련 Kafka 메시지 처리
    @KafkaListener(topics = "ENVIRONMENT", groupId = "environment-consumer-group")
    public void consumeEnvironment(String message) {
        log.info("📩 [ENVIRONMENT] Kafka 메시지 수신: {}", message);
        handleMessage(message, "ENVIRONMENT");
    }

    // 공통 메시지 파싱 및 처리 전달
    private void handleMessage(String message, String topic) {
        try {
            SensorKafkaDto dto = objectMapper.readValue(message, SensorKafkaDto.class);
            log.info("✅ Kafka 메시지 파싱 완료: sensorId={}, zoneId={}, val={}",
                    dto.getSensorId(), dto.getZoneId(), dto.getVal());
            sensorEventProcessor.process(dto, topic);
            log.info("✅ Kafka 메시지 처리 위임 완료: topic={}", topic);
        } catch (Exception e) {
            log.error("❌ Kafka 메시지 파싱 또는 처리 실패: {}", message, e);
        }
    }

}