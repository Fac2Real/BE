package com.factoreal.backend.domain.equip.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
// 설비 점검 예상일 조회 응답 DTO (FastAPI에서 받아온 데이터)
public class MaintenancePredictionResponse {
    // 예상 점검일
    private LocalDate expectedMaintenanceDate;
} 