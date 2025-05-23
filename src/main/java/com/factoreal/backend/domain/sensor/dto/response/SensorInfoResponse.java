package com.factoreal.backend.domain.sensor.dto.response;

import com.factoreal.backend.domain.sensor.entity.Sensor;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensorInfoResponse {
    private String sensorId;
    private String sensorType;
    private String zoneId;
    private String equipId;
    private Double sensorThres;  // 임계치
    private Double allowVal;     // 허용치
    private Integer isZone;

    public static SensorInfoResponse from (Sensor sensor) {
        if (sensor == null) return null;

        return SensorInfoResponse.builder()
                .sensorId(sensor.getSensorId())
                .sensorType(sensor.getSensorType().toString())
                .zoneId(sensor.getZone().getZoneId())
                .equipId(sensor.getEquip().getEquipId())
                .sensorThres(sensor.getSensorThres())
                .allowVal(sensor.getAllowVal())
                .isZone(sensor.getIsZone())
                .build();
    }
}