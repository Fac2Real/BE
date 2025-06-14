package com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbnDetailResponse {
    private Long abnormalId;
    private Integer dangerLevel;
    private String abnormalType;
    private String targetDetail;
    private Double abnVal;
    private LocalDateTime detectedAt;
    private ControlInfoResponse control;  // null = 미대응
}
