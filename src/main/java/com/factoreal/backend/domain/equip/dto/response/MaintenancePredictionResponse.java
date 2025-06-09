package com.factoreal.backend.domain.equip.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
// 설비 점검 예상일 조회 응답 DTO (FastAPI에서 받아온 데이터)
public class MaintenancePredictionResponse {

    private String status; // "ok" or "error" 등
    private List<Double> predictions; // // ML 모델이 예측한 잔존 수명 (남은 일수)
} 