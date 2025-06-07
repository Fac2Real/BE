package com.factoreal.backend.domain.equip.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// 설비 점검 예상일 조회 응답 DTO (FastAPI에서 받아온 데이터)
public class MaintenancePredictionResponse {
    @JsonProperty("remaining_days")
    private Integer remainingDays; // ML 모델이 예측한 잔존 수명 (남은 일수)
} 