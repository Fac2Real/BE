package com.factoreal.backend.domain.abnormalLog.dto.response;

import com.factoreal.backend.domain.abnormalLog.dto.LogType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AbnormalLogResponse {
    private Long id;
    private LogType targetType;
    private String targetId;
    private String abnormalType;
    private Double abnVal;
    private LocalDateTime detectedAt;
    private String zoneId;       // zone 정보를 자세히 담고 싶으면 zoneName 등 추가 가능
    private String zoneName;   // 예: "공장 A의 2번 존"
}
