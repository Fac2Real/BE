package com.factoreal.backend.messaging.kafka.strategy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SensorType {
    temp("온도 센서"),
    humid("습도 센서"),
    vibration("진동 센서"),
    power("전력 센서"),
    current("전류 센서"),
    pressure("압력 센서"),
    dust("먼지 센서"),
    voc("휘발성유기화합물 센서");

    private final String koName;
    public static SensorType getSensorType(String sensorType){
        return SensorType.valueOf(sensorType);
    }
}