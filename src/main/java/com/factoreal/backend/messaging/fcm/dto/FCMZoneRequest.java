package com.factoreal.backend.messaging.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "공간 안전을 위한 요청", description = "공간내의 모든 작업자에게 발송합니다.")
public class FCMZoneRequest {
    @Schema(description = "위험한 공간 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String zoneId;

    @Schema(description = "위험 레벨, 해당 공간의 마지막 dangerLevel과 일치하는 로그를 찾아 사용자에게 어떤 종류의 위험인지 알려주기 위함.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer dangerLevel;
}
