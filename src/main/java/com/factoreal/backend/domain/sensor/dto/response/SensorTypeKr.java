package com.factoreal.backend.domain.sensor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SensorTypeKr {
    temp("온도 센서"),
    humid("습도 센서"),
    vibration("진동 센서"),
    power("전력 센서"),
    current("전류 센서"),
    pressure("압력 센서"),
    dust("먼지 센서"),
    voc("휘발성유기화합물 센서");

    private final String koName;
}