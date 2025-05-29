package com.factoreal.backend.messaging.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "공간 안전을 위한 요청", description = "공간내의 모든 작업자에게 발송합니다.")
public class FCMZoneRequest {
    @Schema(description = "위험한 공간 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String zoneId;
}
