package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerBlockResponse {
    private String workerId;
    private String name;
    private String phone;
    private int workerCnt;          // 이 작업자의 이상치 건수
    private List<AbnDetailResponse> workerAbnormals;
}
