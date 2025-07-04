package com.factoreal.backend.domain.worker.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// 작업자 생성 요청 DTO (FE -> BE)
public class CreateWorkerRequest {
  @Pattern(regexp = "^\\d{8}$")
  private String workerId; // 작업자 ID (사원 번호)
  @NotBlank
  private String name; // 작업자 이름
  private String phoneNumber; // 연락처
  private String email; // 이메일
  private List<String> zoneNames; // 출입 가능한 공간명 리스트
}