package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneBlock {
    private String zoneId;
    private String zoneName;

    // 세부 카운트
    private int    envCnt;  // 환경 이상치 합
    private int    workerCnt;   // 작업자 이상치 합
    private int    facCnt;      // 설비에 걸린 이상치 합
    private int    totalCnt;    // env+worker+fac

    private List<AbnDetail> envAbnormals;      // targetType = SENSOR (환경용)만
    private List<EquipBlock> equips;           // 설비별 블록
    private List<AbnDetail> workerAbnormals;   // targetType = WORKER (작업자)만

}
