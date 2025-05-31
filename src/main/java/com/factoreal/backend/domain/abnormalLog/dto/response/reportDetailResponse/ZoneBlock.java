package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneBlock {
    String zoneId;
    String zoneName;
    int totalCnt;                      // 해당 Zone 전체 이상치 수
    List<AbnDetail> envAbnormals;      // targetType = SENSOR (환경용)만
    List<EquipBlock> equips;           // 설비별 블록
}
