package com.factoreal.backend.domain.abnormalLog.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeSummaryResponse {
//    private String latest30days;
    private String type;        // Worker | Sensor | Equip
    private String grade;       // A / B / C
    private long warnCnt;       // 경고 횟수 참고
    private long dangerCnt;     // 위험 횟수 참고
}