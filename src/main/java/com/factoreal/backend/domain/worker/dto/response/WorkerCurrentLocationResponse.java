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
public class WorkerCurrentLocationResponse extends WorkerInfoResponse{
    private String currentZoneId;
    private String currentZoneName;

    public static WorkerCurrentLocationResponse from(Worker worker, Zone currentZone, Boolean isManager, int status) {
        return WorkerCurrentLocationResponse.builder()
                .workerId(worker.getWorkerId())
                .name(worker.getName())
                .phoneNumber(worker.getPhoneNumber())
                .email(worker.getEmail())
                .fcmToken(worker.getFcmToken())
                .isManager(isManager)
                .status(status)
                .currentZoneId(currentZone != null ? currentZone.getZoneId() : null)
                .currentZoneName(currentZone != null ? currentZone.getZoneName() : null)
                .build();
    }
} 