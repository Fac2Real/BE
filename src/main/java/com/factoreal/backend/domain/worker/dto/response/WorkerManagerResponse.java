package com.factoreal.backend.domain.worker.dto.response;

import com.factoreal.backend.domain.worker.entity.Worker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 공간 담당자 정보 응답 DTO (BE -> FE)
// - 담당자 후보 목록 조회
// - 담당자 정보 조회
public class WorkerManagerResponse {
  private String workerId; // 직원 아이디
  private String name; // 직원 이름
  private String email; // 이메일
  private String phoneNumber; // 전화번호
  private Boolean isManager; // 공간담당자 여부

  public static WorkerManagerResponse fromEntity(Worker worker, Boolean isManager) {
    return WorkerManagerResponse.builder()
        .workerId(worker.getWorkerId())
        .name(worker.getName())
        .email(worker.getEmail())
        .phoneNumber(worker.getPhoneNumber())
        .isManager(isManager)
        .build();
  }
}
