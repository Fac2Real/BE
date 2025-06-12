//package com.factoreal.backend.domain.equip.api;
//
//import com.factoreal.backend.domain.equip.application.EquipMaintenanceService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@Tag(
//        name = "Scheduler Test",
//        description = "설비 점검일 예측 스케줄러 강제 실행 테스트 API<br>일정 주기로 실행되는 배치 스케줄러(fetchAndProcessMaintenancePredictions)를 즉시 실행합니다.<br>운영에서는 사용 주의!"
//)
//@RestController
//@RequestMapping("/test")
//@RequiredArgsConstructor
//public class EquipSchedulerTestController {
//    private final EquipMaintenanceService equipMaintenanceService;
//
//    @Operation(
//            summary = "설비 점검일 예측 스케줄러 수동 실행",
//            description = "설비별 잔존수명(예상 점검일) 예측을 담당하는 배치 스케줄러(fetchAndProcessMaintenancePredictions)를 강제로 즉시 실행합니다.<br>" +
//                    "스케줄러의 배치 기능이 정상 동작하는지 테스트하거나, 개발 환경에서 수동 호출 용도로 사용하세요.<br>" +
//                    "※ 운영 환경에서는 주의해서 사용하시기 바랍니다."
//    )
//    @PostMapping("/run-maintenance-scheduler")
//    public String runSchedulerNow() {
//        equipMaintenanceService.fetchAndProcessMaintenancePredictions(zoneId, String zoneName, String equipId, String equipName);
//        return "Scheduler 실행 완료!";
//    }
//}
