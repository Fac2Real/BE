package com.factoreal.backend.domain.zone.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
// Wearable 장치에서 받아오는 데이터 by 우영. 추후 논의 예정
public class ZoneHistoryRequest {
    private String workerId;
    private String zoneId;
    private LocalDateTime timestamp;
}