package com.factoreal.backend.domain.worker.api;

import com.factoreal.backend.domain.worker.application.WorkerService;
import com.factoreal.backend.domain.worker.dto.request.CreateWorkerRequest;
import com.factoreal.backend.domain.worker.dto.response.WorkerDetailResponse;
import com.factoreal.backend.domain.worker.dto.response.WorkerInfoResponse;
import com.factoreal.backend.domain.worker.dto.response.ZoneManagerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "작업자 API", description = "작업자 조회 API")
public class WorkerController {
    private final WorkerService workerService;

    @Operation(summary = "작업자 생성", description = "새로운 작업자를 생성하고 접근 가능한 공간들을 선택합니다.")
    @PostMapping
    public ResponseEntity<Void> createWorker(@Valid @RequestBody CreateWorkerRequest request) {
        log.info("작업자 생성 요청: {}", request);
        workerService.createWorker(request);
        return ResponseEntity.ok().build(); // 작업자 생성 성공 시 200 응답
    }

    @Operation(summary = "전체 작업자 목록 조회", description = "전체 작업자 목록과 각 작업자의 상태 및 위치 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<WorkerDetailResponse>> getAllWorkers() {
        log.info("전체 작업자 목록 조회 요청");
        List<WorkerDetailResponse> workers = workerService.getAllWorkers();
        return ResponseEntity.ok(workers);
    }

    @Operation(summary = "공간별 작업자 목록 조회", description = "공간 ID를 기반으로 현재 해당 공간에 들어가있는 작업자 리스트를 조회합니다.")
    @GetMapping("/zone/{zoneId}")
    public List<WorkerInfoResponse> getWorkersByZoneId(@PathVariable String zoneId) {
        log.info("공간 ID: {}의 작업자 목록 조회 요청", zoneId);
        List<WorkerInfoResponse> response =  workerService.getWorkersByZoneId(zoneId);
        return response;
    }

    @Operation(summary = "공간 담당자와 담당자의 현재 위치정보 조회", description = "공간 ID를 기반으로 해당 공간의 담당자와 현재 위치 정보를 조회합니다.")
    @GetMapping("/zone/{zoneId}/manager")
    public ZoneManagerResponse getZoneManager(@PathVariable String zoneId) {
        log.info("공간 ID: {}의 담당자 정보 조회 요청", zoneId);

        ZoneManagerResponse response = workerService.getZoneManagerWithLocation(zoneId);
        return response;
    }

    @Operation(summary = "작업자의 정보를 수정합니다.", description = "요청한 정보를 토대로 (이름, 휴대폰번호, 이메일, 출입권한공간리스트) 정보 수정")
    @PostMapping("/update")
    public ResponseEntity<Void> updateWorker(@Valid @RequestBody CreateWorkerRequest req){
        workerService.updateWorker(req);
        return ResponseEntity.ok().build();
    }
}