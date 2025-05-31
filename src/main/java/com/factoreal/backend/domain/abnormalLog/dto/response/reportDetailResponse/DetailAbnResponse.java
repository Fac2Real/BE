package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;

import java.util.List;

public class DetailAbnResponse {
    private String month;                       // "2025-04"

    private List<AbnormalLogResponse> equip;     // 타입별 경고/위험 건수
    private List<AbnormalLogResponse> worker;     // 타입별 경고/위험 건수
    private List<AbnormalLogResponse> zone;     // 타입별 경고/위험 건수

    private String grade;       // A / B / C
    private long warnCnt;       // 경고 횟수 참고
    private long dangerCnt;     // 위험 횟수 참고
}
