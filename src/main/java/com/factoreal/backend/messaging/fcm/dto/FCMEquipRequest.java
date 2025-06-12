package com.factoreal.backend.messaging.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(title = "설비 점검을 위한 요청",description = "작업자와 설비 ID를 지정하여 요청합니다.")
@Data
public class FCMEquipRequest {
    @Schema(description = "설비 점검을 해야하는 작업자 ID",example = "",requiredMode = Schema.RequiredMode.REQUIRED)
    private String workerId;

    @Schema(description = "점검할 설비 ID", example = "", requiredMode = Schema.RequiredMode.REQUIRED)
    private String equipId;

    @Schema(description = "추가로 보낼 메세지", example = "", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String message;
}
