package com.factoreal.backend.domain.abnormalLog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "\uD398\uC774\uC9D5 \uC815\uBCF4 DTO")
public class AbnormalPagingRequest {
    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    private int page;
    
    @Schema(description = "페이지 크기", example = "10")
    private int size;
}
