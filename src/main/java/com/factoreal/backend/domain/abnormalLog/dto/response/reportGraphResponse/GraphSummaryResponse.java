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
    /**
     * 로그 갯수 관련
     */
    private int totalCnt;

    private int warnCnt;

    private int dangerCnt;

    /**
     * SENSOR / EQUIP / WORKER 건수
     */
    private List<BarResponse> typeStats;

    /**
     * 30 일 구간 MM-dd 별 건수
     */
    private List<BarResponse> dateStats;

    /**
     * Zone 별 건수
     */
    private List<BarResponse> zoneStats;
}