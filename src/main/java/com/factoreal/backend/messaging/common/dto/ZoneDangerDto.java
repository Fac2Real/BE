package com.factoreal.backend.messaging.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZoneDangerDto {
    private String zoneId;
    private String sensorType;
    private int level;
}
