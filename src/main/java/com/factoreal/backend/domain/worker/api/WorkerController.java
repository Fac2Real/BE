package com.factoreal.backend.domain.worker.api;

import com.factoreal.backend.domain.worker.dto.response.WorkerInfoResponse;
import com.factoreal.backend.domain.worker.dto.response.ZoneManagerResponse;
import com.factoreal.backend.domain.worker.application.WorkerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "작업자 API", description = "작업자 조회 API")
public class WorkerController {
    private final WorkerService workerService;
    
    @Operation(summary = "전체 작업자 목록 조회", description = "전체 작업자 목록을 조회합니다.")
    @GetMapping
    public List<WorkerInfoResponse> getAllWorkers() {
        log.info("전체 작업자 목록 조회 요청");
        return workerService.getAllWorkers();
    }
    
    @Operation(summary = "공간별 작업자 목록 조회", description = "공간 ID를 기반으로 현재 해당 공간에 들어가있는 작업자 리스트를 조회합니다.")
    @GetMapping("/zone/{zoneId}")
    public List<WorkerInfoResponse> getWorkersByZoneId(@PathVariable String zoneId) {
        log.info("공간 ID: {}의 작업자 목록 조회 요청", zoneId);
        return workerService.getWorkersByZoneId(zoneId);
    }

    @Operation(summary = "공간 담당자 정보 조회", 
              description = "공간 ID를 기반으로 해당 공간의 담당자와 현재 위치 정보를 조회합니다.")
    @GetMapping("/zone/{zoneId}/manager")
    public ZoneManagerResponse getZoneManager(@PathVariable String zoneId) {
        log.info("공간 ID: {}의 담당자 정보 조회 요청", zoneId);
        return workerService.getZoneManagerWithLocation(zoneId);
    }
} 