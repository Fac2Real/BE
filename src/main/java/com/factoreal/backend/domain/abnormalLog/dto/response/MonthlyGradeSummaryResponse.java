package com.factoreal.backend.domain.abnormalLog.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyGradeSummaryResponse {
    private String period;                       // "2025-04"
    private List<GradeSummaryResponse> abnormalInfos;  // 타입별 A/B/C
}