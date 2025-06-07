package com.factoreal.backend.domain.equip.api;

import com.factoreal.backend.domain.equip.application.EquipMaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "(JUnit 테스트용) 설비 점검일자 기한 알림", description = "머신러닝 설비의 예상 점검일자 관련 API")
@RestController
@RequestMapping("/api/equip-maintenance")
@RequiredArgsConstructor
public class EquipMaintenanceController {

    private final EquipMaintenanceService equipMaintenanceService;

    @Operation(summary = "공장 관리자에게 설비 점검일자 D-5, D-3 때마다 알림 발송", description = "모든 설비의 예상 점검일을 확인하고 필요시 알림을 발송합니다.")
    @PostMapping("/check")
    public ResponseEntity<String> checkMaintenanceDates() {
        equipMaintenanceService.fetchAndProcessMaintenancePredictions();
        return ResponseEntity.ok("점검일 확인 완료");
    }
}