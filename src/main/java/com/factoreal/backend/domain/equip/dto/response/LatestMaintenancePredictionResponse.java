package com.factoreal.backend.domain.equip.dto.response;

import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.equip.entity.EquipHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 최근 예상 점검일자와 남은 일수 반환 (BE -> FE)
public class LatestMaintenancePredictionResponse {
    private String equipId;
    private String equipName;
    private String zoneId;
    private String zoneName;
    private LocalDate expectedMaintenanceDate;
    private Long daysUntilMaintenance;

    public static LatestMaintenancePredictionResponse fromEntity(Equip equip, EquipHistory history, Long daysUntilMaintenance) {
        return LatestMaintenancePredictionResponse.builder()
            .equipId(equip.getEquipId())
            .equipName(equip.getEquipName())
            .zoneId(equip.getZone().getZoneId())
            .zoneName(equip.getZone().getZoneName())
            .expectedMaintenanceDate(history.getAccidentDate())
            .daysUntilMaintenance(daysUntilMaintenance)
            .build();
    }
} 