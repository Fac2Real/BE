package com.factoreal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// Kafka mapper용 DTO
// Todo : Infra Flink dangerLevel 추가 후 변경 예정
public class SensorKafkaDto {
    private String zoneId;
    private String equipId;
    private String sensorId;
    private String sensorType;
    private Double val;  // 측정값 단위
    private String time; // 센서 생성시간
}
