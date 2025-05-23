package com.factoreal.backend.domain.equip.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipInfoResponse {
    private String equipId;
    private String equipName;
    private String zoneName;
    private String zoneId;
}