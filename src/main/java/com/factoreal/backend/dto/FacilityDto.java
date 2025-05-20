package com.factoreal.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * 설비 정보 구조화 DTO
 * 설비의 기본 정보와 포함된 센서 정보를 구조화합니다.
 */
@Getter            // ← 모든 필드 Getter
@Setter            // ← Setter (원하면 삭제)
@Builder           // ← ⭐ Builder 자동 생성
@AllArgsConstructor
@NoArgsConstructor
public class FacilityDto {

    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private String id;
    @JsonProperty("fac_sensor")
    private List<SensorDto> facSensor;
}
