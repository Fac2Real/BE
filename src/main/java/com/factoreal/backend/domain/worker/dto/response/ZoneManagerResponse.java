package com.factoreal.backend.domain.worker.dto.response;

import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.entity.Zone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneManagerResponse {
    private String workerId;
    private String name;
    private String phoneNumber;
    private String email;
    private String currentZoneId;
    private String currentZoneName;

    public static ZoneManagerResponse from(Worker worker, Zone currentZone) {
        return ZoneManagerResponse.builder()
                .workerId(worker.getWorkerId())
                .name(worker.getName())
                .phoneNumber(worker.getPhoneNumber())
                .email(worker.getEmail())
                .currentZoneId(currentZone != null ? currentZone.getZoneId() : null)
                .currentZoneName(currentZone != null ? currentZone.getZoneName() : null)
                .build();
    }
} 