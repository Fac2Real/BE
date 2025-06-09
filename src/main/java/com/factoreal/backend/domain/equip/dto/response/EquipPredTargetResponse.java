package com.factoreal.backend.domain.equip.dto.response;

import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.zone.entity.Zone;
import lombok.*;


/**
 * 설비 추론 대상 정보를 응답으로 내려주는 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EquipPredTargetResponse {
    private String equipId;
    private String equipName;
    private String zoneId;
    private String zoneName;

    // 엔티티(Equip)와 존(Zone) 정보를 바탕으로 DTO 생성
    public static EquipPredTargetResponse fromEntity(Equip equip, Zone zone) {
        return EquipPredTargetResponse.builder()
                .equipId(equip.getEquipId())
                .equipName(equip.getEquipName())
                .zoneId(equip.getZone() != null ? equip.getZone().getZoneId() : null)
                .zoneName(zone != null ? zone.getZoneName() : null)
                .build();
    }
}
