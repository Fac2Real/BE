package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbnDetail {
    Long abnormalId;
    Integer dangerLevel;
    String abnormalType;
    Double abnVal;
    LocalDateTime detectedAt;
    ControlInfo control;  // null = 미대응
}
