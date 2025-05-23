package com.factoreal.backend.domain.equip.api;

import com.factoreal.backend.domain.equip.dto.request.EquipCreateRequest;
import com.factoreal.backend.domain.equip.application.EquipService;
import com.factoreal.backend.domain.equip.dto.request.EquipUpdateRequest;
import com.factoreal.backend.domain.equip.dto.response.EquipInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equips")
@Tag(name = "설비 정보 API", description = "설비 정보 관리 API")
@RequiredArgsConstructor
public class EquipController {
    private final EquipService service;

    @PostMapping
    @Operation(summary = "설비 등록", description = "UI로부터 설비명과 공간명을 입력받아 고유 ID를 생성하여 설비 정보를 등록합니다.")
    public EquipInfoResponse createEquip(@Valid @RequestBody EquipCreateRequest equipCreateRequest) {
        return service.createEquip(equipCreateRequest);
    }

    @PostMapping("/{equipId}")
    @Operation(summary = "설비 정보 수정", description = "기존 설비의 이름을 수정합니다.")
    public EquipInfoResponse updateEquip(
            @PathVariable String equipId,
            @RequestBody EquipUpdateRequest dto) {
        return service.updateEquip(equipId, dto);
    }

    @GetMapping
    @Operation(summary = "설비 목록 조회", description = "등록된 모든 설비 정보를 조회합니다.")
    public List<EquipInfoResponse> listEquips() {
        return service.getAllEquips();
    }

//    // 공간별로 구분된 설비 조회
//    @GetMapping("/zones")
//    @Operation(summary = "공간별 설비·센서 매핑 조회", description = "모든 공간(zone)마다 환경센서·설비·설비센서를 구조화해 반환합니다.")
//    public ResponseEntity<List<ZoneDto>> listEquipsByZone() {
//        return ResponseEntity.ok(service.getEquipMapByZone());
//    }

}
