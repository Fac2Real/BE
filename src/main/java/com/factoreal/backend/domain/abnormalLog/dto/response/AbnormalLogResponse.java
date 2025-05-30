package com.factoreal.backend.domain.abnormalLog.dto.response;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AbnormalLogResponse {
    private Long id;
    private TargetType targetType;
    private String targetId;
    private String abnormalType;
    private Double abnVal;
    private Integer dangerLevel;
    private LocalDateTime detectedAt;
    private String zoneId;       // zone 정보를 자세히 담고 싶으면 zoneName 등 추가 가능
    private String zoneName;   // 예: "공장 A의 2번 존"
    /** AbnormalLog 엔티티 → DTO 변환 편의 메서드 */
    public static AbnormalLogResponse from(AbnormalLog log) {
        return AbnormalLogResponse.builder()
                .id(log.getId())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .abnormalType(log.getAbnormalType())
                .abnVal(log.getAbnVal())
                .dangerLevel(log.getDangerLevel())
                .detectedAt(log.getDetectedAt())
                .zoneId(log.getZone() != null ? log.getZone().getZoneId() : null)
                .zoneName(log.getZone() != null ? log.getZone().getZoneName() : null)
                .build();
    }
}
