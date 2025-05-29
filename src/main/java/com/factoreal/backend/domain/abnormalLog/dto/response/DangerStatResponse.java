package com.factoreal.backend.domain.abnormalLog.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DangerStatResponse {
    private String type;   // Worker | Sensor | Equip
    private long warnCnt;  // dangerLevel == 1
    private long dangerCnt;// dangerLevel == 2
}
