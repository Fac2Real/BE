package com.factoreal.backend.domain.equip.dto.request;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 설비 점검일자 수정 요청 DTO (FE -> BE)
public class EquipCheckDateRequest {
    private LocalDate checkDate;  // 설비 점검일자 (YYYY-MM-DD)
} 