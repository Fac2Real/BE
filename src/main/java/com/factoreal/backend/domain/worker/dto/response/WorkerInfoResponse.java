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
public class WorkerInfoResponse {
    private String workerId;
    private String name;
    private String phoneNumber;
    private String email;
    private String fcmToken;
    private Boolean isManager;

    public static WorkerInfoResponse from(Worker worker, Boolean isManager) {
        return WorkerInfoResponse.builder()
                .workerId(worker.getWorkerId())
                .name(worker.getName())
                .phoneNumber(worker.getPhoneNumber())
                .email(worker.getEmail())
                .fcmToken(worker.getFcmToken())
                .isManager(isManager)
                .build();
    }
} 