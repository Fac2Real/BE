package com.factoreal.backend.domain.zone.dto.response;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneLogResponse {
    private String zoneId;
    private String targetType; // 1. 대분류(한글) -> 환경, 설비, 작업자
    private String sensorType; // 3. 대상
    private int dangerLevel; // 2. 위험 레벨(세분류)
    private double value; // 4. 측정값
    private LocalDateTime timestamp; // 5. 발생 시각
    private String abnormalType; // 4. 설명
    private String targetId;

    public static ZoneLogResponse fromEntity(AbnormalLog abnormalLog) {
        return ZoneLogResponse.builder()
                .zoneId(abnormalLog.getZone().getZoneId())
                .targetType(convertLogTypeToKorean(abnormalLog.getTargetType()))
                .sensorType(abnormalLog.getTargetDetail())
                .dangerLevel(abnormalLog.getDangerLevel())
                .value(abnormalLog.getAbnVal())
                .timestamp(abnormalLog.getDetectedAt())
                .abnormalType(abnormalLog.getAbnormalType())
                .targetId(abnormalLog.getTargetId())
                .build();
    }

    private static String convertLogTypeToKorean(TargetType targetType) {
        return switch (targetType) {
            case Sensor -> "환경";
            case Worker -> "작업자";
            case Equip -> "설비";
        };
    }

    private static int calculateDangerLevel(String abnormalType) {
        if (abnormalType.contains("위험")) return 2;
        if (abnormalType.contains("주의")) return 1;
        return 0;
    }
} 

/**
 * {
  "content": [
    {
      "zoneId": "zone123",
      "targetType": "환경",  // 또는 "작업자", "설비"
       "sensorType": "TEMPERATURE",
       "dangerLevel": 2,
       "value": 35.5,
       "timestamp": "2024-03-20T14:30:00",
       "abnormalType": "온도 위험",
       "targetId": "sensor456"
    }
    // ... 더 많은 로그
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 50,
  "totalPages": 5
}
 */