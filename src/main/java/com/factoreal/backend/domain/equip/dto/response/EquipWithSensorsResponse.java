package com.factoreal.backend.domain.equip.dto.response;

import com.factoreal.backend.domain.sensor.dto.response.SensorInfoResponse;
import lombok.*;

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
    private List<SensorInfoResponse> sensors;
} 