package com.factoreal.backend.domain.abnormalLog.dto.response.reportGraphResponse;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphSummaryResponse {

    private String period;

    /** SENSOR / EQUIP / WORKER 건수 */
    private List<Bar> typeStats;

    /** 30 일 구간 MM-dd 별 건수 */
    private List<Bar> dateStats;

    /** Zone 별 건수 */
    private List<Bar> zoneStats;
}