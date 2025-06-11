package com.factoreal.backend.domain.worker.dto.response;

import com.factoreal.backend.domain.worker.entity.Worker;

import com.factoreal.backend.domain.zone.dto.response.ZoneInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class WorkerDetailResponse extends WorkerInfoResponse{
  private String currentZoneId; // 현재 위치한 공간 ID
  private String currentZoneName; // 현재 위치한 공간 이름

  private List<ZoneInfoResponse> accessZones;
  private List<ZoneInfoResponse> managedZones;

  // Entity -> DTO 변환
  public static WorkerDetailResponse fromEntity(Worker worker, Boolean isManager, Integer status, String currentZoneId,
          String currentZoneName,
          List<ZoneInfoResponse> accessZones,
          List<ZoneInfoResponse> managedZones) {
    return WorkerDetailResponse.builder()
        .workerId(worker.getWorkerId())
        .name(worker.getName())
        .phoneNumber(worker.getPhoneNumber())
        .email(worker.getEmail())
        .isManager(isManager)
        .status(status)
        .currentZoneId(currentZoneId)
        .currentZoneName(currentZoneName)
        .accessZones(accessZones)
        .managedZones(managedZones)
        .build();
  }
}
