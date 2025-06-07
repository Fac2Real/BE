package com.factoreal.backend.domain.equip.api;

import com.factoreal.backend.domain.equip.application.EquipMaintenanceService;
import com.factoreal.backend.domain.equip.dto.response.LatestMaintenancePredictionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "설비 점검일자 예측 및 알림", description = "머신러닝 설비의 예상 점검일자 관련 API")
@RestController
@RequestMapping("/api/equip-maintenance")
@RequiredArgsConstructor
public class EquipMaintenanceController {

    private final EquipMaintenanceService equipMaintenanceService;

    @Operation(summary = "(스케줄링) 공장 관리자에게 예상 점검일자가 D-5, D-3 인 경우, 알림 발송", description = "모든 설비의 예상 점검일을 확인하고 필요시 알림을 발송합니다.")
    @PostMapping("/check")
    public String checkMaintenanceDates() {
        equipMaintenanceService.fetchAndProcessMaintenancePredictions();
        return "점검일 확인 완료";
    }

    @Operation(
        summary = "특정 설비의 가장 최근 예상 점검일자 조회",
        description = "DB에 저장된 특정 설비의 가장 최근 예상 점검일자를 조회합니다. " +
            "미점검 이력이 있는 경우 해당 이력의 예상 점검일자를 반환하고, " +
            "모든 이력이 점검 완료된 경우 가장 최근에 저장된 이력의 예상 점검일자를 반환합니다."
    )
    @GetMapping("/latest/{equipId}")
    public LatestMaintenancePredictionResponse getLatestMaintenancePrediction(
        @Parameter(description = "설비 ID") @PathVariable String equipId,
        @Parameter(description = "공간 ID") @RequestParam String zoneId
    ) {
        return equipMaintenanceService.getLatestMaintenancePrediction(equipId, zoneId);
    }
}