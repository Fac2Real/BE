package com.factoreal.backend.messaging.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(title = "요청 성공 여부를 반환",description = "작업자Id, 작업자 명, 성공 여부, 실패 원인을 반환합니다.")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FCMResponse {
    private String workerId;
    private String workerName;
    private boolean success;
    private String errorDescription;
}
