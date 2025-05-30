package com.factoreal.backend.domain.abnormalLog.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyDetailResponse {
    private String month;                       // "2025-04"
    private List<DangerStatResponse> stats;     // 타입별 경고/위험 건수
}