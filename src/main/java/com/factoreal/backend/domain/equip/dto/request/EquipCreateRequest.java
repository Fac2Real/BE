package com.factoreal.backend.domain.equip.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipCreateRequest {
    private String equipName;
    private String zoneName; // 사용자가 선택한 공간명
}