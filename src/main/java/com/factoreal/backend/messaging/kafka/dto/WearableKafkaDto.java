package com.factoreal.backend.messaging.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
/**
 * Kafka Wearable 데이터 mapper용 DTO
 */
public class WearableKafkaDto {
    private String wearableDeviceId;
    private String workerId;
    private String sensorType;
    private Long val;
    private Integer dangerLevel; // 0 이면 정상 1이면 비정상
    private String time;       // 센서 탐지 시간
}
