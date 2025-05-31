package com.factoreal.backend.domain.equip.dto.response;

import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.zone.entity.Zone;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 설비 정보 응답 DTO
public class EquipInfoResponse {
    private String equipId;
    private String equipName;
    private String zoneName;
    private String zoneId;

    public static EquipInfoResponse fromEntity(Equip equip, Zone zone) {
        return EquipInfoResponse.builder()
                .equipId(equip.getEquipId())
                .equipName(equip.getEquipName())
                .zoneName(zone.getZoneName())
                .zoneId(zone.getZoneId())
                .build();
    }
}