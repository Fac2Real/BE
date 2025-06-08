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
    private String sensorTypeKr;   // 한글명   (예: 온도)
    private String zoneId;
    private String equipId;
    private Double sensorThres;  // 임계치
    private Double allowVal;     // 허용치
    private Integer isZone;

    public static SensorInfoResponse fromEntity(Sensor sensor) {
        if (sensor == null) return null;

        var typeEnum = sensor.getSensorType();   // enum -> SensorTypeKr
        var koName = SensorTypeKr.valueOf(typeEnum.name()) // enum 이름으로 매핑
                .getKoName();

        return SensorInfoResponse.builder()
                .sensorId(sensor.getSensorId())
                .sensorType(sensor.getSensorType().toString())
                .sensorTypeKr(koName)
                .zoneId(sensor.getZone().getZoneId())
                .equipId(sensor.getEquip().getEquipId())
                .sensorThres(sensor.getSensorThres())
                .allowVal(sensor.getAllowVal())
                .isZone(sensor.getIsZone())
                .build();
    }
}