package com.factoreal.backend.domain.zone.api;

import java.util.List;

import com.factoreal.backend.domain.abnormalLog.dto.request.AbnormalPagingRequest;
import com.factoreal.backend.domain.zone.dto.response.ZoneLogResponse;
import com.factoreal.backend.domain.zone.dto.request.ZoneCreateRequest;
import com.factoreal.backend.domain.zone.dto.request.ZoneUpdateRequest;
import com.factoreal.backend.domain.zone.dto.response.ZoneDetailResponse;
import com.factoreal.backend.domain.zone.dto.response.ZoneInfoResponse;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import com.factoreal.backend.domain.zone.application.ZoneService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
@Tag(name = "공간 정보 API", description = "공간(Zone) 매핑 처리 API")
public class ZoneController {
    private final ZoneService service;
    
    @PostMapping
    @Operation(summary = "공간 생성", description = "UI에서 입력한 공간명으로 Zone을 등록하고 고유 zoneId를 생성하여 반환합니다.")
    public ZoneInfoResponse createZone (@RequestBody ZoneCreateRequest zoneCreateRequest) {
        return service.createZone(zoneCreateRequest);
    }

    @PostMapping("/{zoneName}")
    @Operation(summary = "공간 정보 수정", description = "기존 공간의 이름을 수정합니다.")
    public ZoneInfoResponse updateZone(
            @PathVariable String zoneName,
            @RequestBody ZoneUpdateRequest dto) {
        return service.updateZone(zoneName, dto);
    }

    @GetMapping
    @Operation(summary = "공간 리스트 조회", description = "등록된 모든 공간 정보를 조회합니다.")
    public List<ZoneInfoResponse> listZones() {
        return service.getAllZones();
    }

    @GetMapping("/zoneitems")
    @Operation(summary = "공간별 설비,센서 데이터 조회", description = "등록된 공간들의 각 정보를 조회합니다.")
    public List<ZoneDetailResponse> listZoneItems() {
        return service.getZoneItems();
    }

    @Operation(summary = "공간별 시스템 로그 조회", description = "특정 공간(zone)의 시스템 로그를 페이징 처리하여 조회합니다.")
    @GetMapping("/{zoneId}/logs")
    public Page<ZoneLogResponse> getSystemLogsByZone(
            @Parameter(description = "조회할 공간 ID", required = true)
            @PathVariable String zoneId,
            @Parameter(description = "페이징 정보 (page: 페이지 번호, size: 페이지 크기)")
            @ModelAttribute AbnormalPagingRequest pagingDto) {
        return service.findSystemLogsByZoneId(zoneId, pagingDto);
    }
}