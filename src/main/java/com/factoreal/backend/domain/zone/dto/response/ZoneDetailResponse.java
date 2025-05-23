package com.factoreal.backend.domain.zone.dto.response;

import com.factoreal.backend.domain.equip.dto.response.EquipDetailResponse;
import com.factoreal.backend.domain.sensor.dto.response.SensorInfoResponse;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZoneDetailResponse {
    private String zoneName;
    private List<SensorInfoResponse> zoneSensorList;
    private List<EquipDetailResponse> equipList;
}
