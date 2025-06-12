package com.factoreal.backend.domain.worker.api;

import com.factoreal.backend.domain.worker.dto.response.WorkerInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.factoreal.backend.domain.worker.application.WorkerManagerService;
import com.factoreal.backend.domain.worker.dto.response.WorkerManagerResponse;

import java.util.List;

@Tag(name = "공간 담당자 API", description = "공간별 담당자 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/zone-managers")
@RequiredArgsConstructor
public class WorkerManagerController {

  private final WorkerManagerService workerManagerService;

  @Operation(summary = "공간 담당자 후보 목록 조회", description = "특정 공간의 담당자로 지정 가능한 작업자 목록을 조회합니다.")
  @GetMapping("/candidates/{zoneId}")
  public ResponseEntity<List<WorkerManagerResponse>> getManagerCandidates(
      @Parameter(description = "공간 ID", required = true) @PathVariable String zoneId) {
    log.info("공간 ID: {}의 담당자 후보 목록 조회 요청", zoneId);
    return ResponseEntity.ok(workerManagerService.getManagerCandidates(zoneId));
  }

  @Operation(summary = "공간 담당자 지정", description = "특정 공간의 담당자를 지정합니다.")
  @PostMapping("/{zoneId}/assign/{workerId}")
  public ResponseEntity<String> assignManager(
      @Parameter(description = "공간 ID", required = true) @PathVariable String zoneId,
      @Parameter(description = "작업자 ID", required = true) @PathVariable String workerId) {
    workerManagerService.assignManager(zoneId, workerId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "현재 공간 담당자 정보 조회", description = "특정 공간의 현재 담당자 정보를 조회합니다.")
  @GetMapping("/{zoneId}")
  public WorkerInfoResponse getCurrentManager(
      @Parameter(description = "공간 ID", required = true) @PathVariable String zoneId) {
    log.info("공간 ID: {}의 현재 담당자 조회 요청", zoneId);
    return  workerManagerService.getCurrentManager(zoneId);
  }
}
