package com.factoreal.backend.domain.worker.dto.response;

import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.entity.Zone;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneManagerResponseDto extends ZoneManagerResponse{
    private String status; // 공간 담당자의 현재 상태
    private String zone; // 공간 담당자의 현재 위치
    public static ZoneManagerResponseDto from(Worker worker, Zone currentZone, String status, String zone) {
        return ZoneManagerResponseDto.builder()
            .workerId(worker.getWorkerId())
            .name(worker.getName())
            .phoneNumber(worker.getPhoneNumber())
            .email(worker.getEmail())
            .currentZoneId(currentZone != null ? currentZone.getZoneId() : null)
            .currentZoneName(currentZone != null ? currentZone.getZoneName() : null)
            .status(status)
            .zone(zone)
            .build();
    }
}
