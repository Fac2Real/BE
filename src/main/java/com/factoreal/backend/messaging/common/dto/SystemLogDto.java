package com.factoreal.backend.messaging.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogDto {
    private String zoneId;
    private String zoneName;
    private String sensorType;
    private int dangerLevel; // 위험도 수준
    private double value; // 측정값
    private String timestamp; // 로그 생성 시간
}
/**
 * {
  "zoneId": "20250507171046-862",
  "zoneName": "포장 구역 B",
  "sensorType": "temp",
  "dangerLevel": 2,
  "value": 25.5,
  "timestamp": "2025-05-09T15:22:31"
}
 */