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
    // 작업자의 위치와 상태 추가
    private String status;
    private String zone;
    public static WorkerDetailResponse from(Worker worker, Boolean isManager, String status, String zone) {
        return WorkerDetailResponse.builder()
            .workerId(worker.getWorkerId())
            .name(worker.getName())
            .phoneNumber(worker.getPhoneNumber())
            .email(worker.getEmail())
            .fcmToken(worker.getFcmToken())
            .status(status)
            .zone(zone)
            .isManager(isManager)
            .build();
    }
}
