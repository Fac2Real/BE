package com.factoreal.backend.domain.equip.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipUpdateRequest {
    private String equipName; // 수정할 설비명
}
