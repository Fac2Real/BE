package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipBlockResponse {
    private String equipId;
    private String equipName;

    private int facCnt;          // 이 설비의 이상치 건수
    private List<AbnDetailResponse> facAbnormals;         // 그 설비의 이상치들
}
