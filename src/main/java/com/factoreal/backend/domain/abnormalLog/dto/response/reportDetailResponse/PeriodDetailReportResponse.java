package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodDetailReportResponse {
    private String period;                     // "2025.04.30 ~ 2025.05.30"
    private List<ZoneBlockResponse> zones;             // 공간 단위 블록
}
