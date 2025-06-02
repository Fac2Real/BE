package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlInfo {
    private LocalDateTime executedAt;
    private String controlType;
    private Double controlVal;
    private Integer controlStat;
}
