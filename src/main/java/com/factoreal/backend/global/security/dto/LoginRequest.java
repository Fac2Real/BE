package com.factoreal.backend.global.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoginRequest {
    @Schema(defaultValue = "monitory")
    private String username;
    @Schema(defaultValue = "monitory")
    private String password;
}
