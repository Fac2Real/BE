package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipBlock {
    String equipId;
    String equipName;
    int totalCnt;                      // 설비별 이상치 수
    List<AbnDetail> logs;         // 그 설비의 이상치들
}
