package com.factoreal.backend.domain.sensor.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SensorUpdateRequest {
    // private String sensorPurpose;  // 센서목적
    // private String location;       // 위치
    private Double sensorThres;      // 임계치
    private Double allowVal;       // 허용치(오차범위)
}