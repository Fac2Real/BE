package com.factoreal.backend.messaging.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FCMTokenRegistDto {
    @NotBlank(message="토큰을 입력해야 합니다.")
    @Schema(description = "FCM Token", example = "")
    String token;
    @NotBlank(message="작업자ID를 입력해야 합니다.")
    @Schema(description = "Worker Id", example = "")
    String workerId;
}
