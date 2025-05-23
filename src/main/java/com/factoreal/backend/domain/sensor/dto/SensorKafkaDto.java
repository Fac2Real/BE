package com.factoreal.backend.domain.sensor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
/**
 * Kafka Sensor 데이터 mapper용 DTO
 */
public class SensorKafkaDto {
    private String zoneId;     // 공간 ID
    private String equipId;    // 설비 ID
    private String sensorId;   // 센서 ID
    private String sensorType; // 센서 타입
    private Double val;        // 센서 측정값
    private String Category;   // 센서 카테고리 {ENVIRONMENT, EQUIPMENT}
    private String time;       // 센서 탐지 시간
    private int dangerLevel;   // 위험도
}
