package com.factoreal.backend.domain.equip.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 설비명 수정 요청 DTO (FE -> BE)
public class EquipUpdateRequest {
    private String equipName;      // 수정할 설비명
}
