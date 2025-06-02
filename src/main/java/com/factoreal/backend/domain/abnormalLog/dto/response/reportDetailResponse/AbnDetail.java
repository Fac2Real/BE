package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbnDetail {
    private Long abnormalId;
    private Integer dangerLevel;
    private String abnormalType;
    private Double abnVal;
    private LocalDateTime detectedAt;
    private ControlInfo control;  // null = 미대응
}
