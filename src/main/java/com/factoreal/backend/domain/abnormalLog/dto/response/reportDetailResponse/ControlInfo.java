package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlInfo {
    LocalDateTime executedAt;
    String controlType;
    Double controlVal;
    Integer controlStat;
}
