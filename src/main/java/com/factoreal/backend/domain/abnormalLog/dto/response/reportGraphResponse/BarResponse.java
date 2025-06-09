package com.factoreal.backend.domain.abnormalLog.dto.response.reportGraphResponse;

import lombok.*;

// Bar.java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarResponse {
    private String label;   // x 축
    private long cnt;     // y 축
}
