package com.factoreal.backend.messaging.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "도움이 필요한 작업자ID", description = "위험한 작업자 ID")
@Data
public class FCMSafetyRequest {
    @Schema(description = "도움을 줄 수 있는 작업자 ID",example = "",requiredMode = Schema.RequiredMode.REQUIRED)
    private String workerId;
    
    @Schema(description = "도움이 필요한 작업자 ID", example = "", requiredMode = Schema.RequiredMode.REQUIRED)
    private String careNeedWorkerId;

    @Schema(description = "추가로 보낼 메세지", example = "", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String message;
}
