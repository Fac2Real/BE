package com.factoreal.backend.domain.zone.api;

import com.factoreal.backend.domain.zone.application.ZoneHistoryService;
import com.factoreal.backend.domain.zone.dto.request.ZoneHistoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "작업자 위치 API", description = "작업자의 실시간 위치 정보를 관리하는 API")
@RestController
@RequestMapping("/api/zone-history")
@RequiredArgsConstructor
public class ZoneHistoryController {
    private final ZoneHistoryService zoneHistoryService;

    @Operation(summary = "작업자 위치 업데이트", description = "웨어러블 디바이스로부터 받은 작업자의 위치 정보를 업데이트합니다.")
    @PostMapping("/update")
    public ResponseEntity<Void> updateWorkerLocation(@RequestBody ZoneHistoryRequest request) {
        log.info("작업자 위치 업데이트 요청: {}", request);
        zoneHistoryService.updateWorkerLocation(
                request.getWorkerId(),
                request.getZoneId(),
                request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now()
        );
        return ResponseEntity.ok().build();
    }
}