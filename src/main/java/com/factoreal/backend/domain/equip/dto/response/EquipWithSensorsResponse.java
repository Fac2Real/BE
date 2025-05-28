package com.factoreal.backend.domain.equip.dto.response;

import com.factoreal.backend.domain.sensor.dto.response.SensorInfoResponse;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.zone.entity.Zone;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 설비 정보와 센서 정보를 함께 반환하는 DTO (BE -> FE)
public class EquipWithSensorsResponse {
    private String equipId;
    private String equipName;
    private String zoneName;
    private String zoneId;
    private LocalDate lastUpdateDate;  // 최근 교체일자
    private LocalDate lastCheckDate;   // 최근 점검일자
    private List<SensorInfoResponse> sensors;

    public static EquipWithSensorsResponse fromEntity(Equip equip, Zone zone, LocalDate lastUpdateDate, LocalDate lastCheckDate, List<SensorInfoResponse> sensors) {
        return EquipWithSensorsResponse.builder()
                .equipId(equip.getEquipId())
                .equipName(equip.getEquipName())
                .zoneName(zone.getZoneName())
                .zoneId(zone.getZoneId())
                .lastUpdateDate(lastUpdateDate)
                .lastCheckDate(lastCheckDate)
                .sensors(sensors)
                .build();
    }
} 