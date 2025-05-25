package com.factoreal.backend.domain.worker.dto.response;

import com.factoreal.backend.domain.worker.entity.Worker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class WorkerDetailResponse extends WorkerInfoResponse{
  private String status; // 작업자 상태
  private String currentZoneId; // 현재 위치한 공간 ID
  private String currentZoneName; // 현재 위치한 공간 이름

  // Entity -> DTO 변환
  public static WorkerDetailResponse fromEntity(Worker worker, Boolean isManager, Integer status, String currentZoneId,
          String currentZoneName) {
    return WorkerDetailResponse.builder()
        .workerId(worker.getWorkerId())
        .name(worker.getName())
        .phoneNumber(worker.getPhoneNumber())
        .email(worker.getEmail())
        .isManager(isManager)
        .status(status != null ? status.toString() : null)
        .currentZoneId(currentZoneId)
        .currentZoneName(currentZoneName)
        .build();
  }
}
