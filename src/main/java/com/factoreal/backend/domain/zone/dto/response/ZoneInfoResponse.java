package com.factoreal.backend.domain.zone.dto.response;

import com.factoreal.backend.domain.zone.entity.Zone;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZoneInfoResponse {
    private String zoneId;
    private String zoneName;

    public static ZoneInfoResponse from(Zone zone) {
        return new ZoneInfoResponse(zone.getZoneId(), zone.getZoneName());
    }
}