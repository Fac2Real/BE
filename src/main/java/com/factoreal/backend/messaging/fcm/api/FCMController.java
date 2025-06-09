package com.factoreal.backend.messaging.fcm.api;

import com.factoreal.backend.domain.notifyLog.dto.TriggerType;
import com.factoreal.backend.messaging.fcm.dto.FCMEquipRequest;
import com.factoreal.backend.messaging.fcm.dto.FCMSafetyRequest;
import com.factoreal.backend.messaging.fcm.dto.FCMTokenRegistDto;
import com.factoreal.backend.messaging.fcm.dto.FCMZoneRequest;
import com.factoreal.backend.messaging.fcm.application.FCMService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/fcm")
@Tag(name = "작업자 호출용 API", description = "모바일 앱 관련 알람 API")
@Slf4j
@RequiredArgsConstructor
public class FCMController {
    private final FCMService fcmService;

    @PostMapping
    @Operation(summary = "FCM 토큰 등록", description = "작업자의 앱을 실행하면 worker_info에 fcm토큰이 저장됩니다.")
    public ResponseEntity<String> sendMessage(@RequestBody FCMTokenRegistDto message) {
        try {
            String response = fcmService.saveToken(message.getWorkerId(), message.getToken());
            log.info("토큰 등록 완료");
            if (response.equals(message.getToken())) {
                return ResponseEntity.ok().body(response);
            }
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "작업자 안전(safety)알람 호출용", description = "상태 이상자를 도울사람에게 호출 전송")
    @PostMapping("/safety")
    public ResponseEntity<Object> sendWorkerMessage(@RequestBody FCMSafetyRequest request) {
//        fcmService.sendMessage();
        fcmService.sendWorkerSafety(request.getWorkerId(), request.getCareNeedWorkerId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "공간 안전(safety)알람 호출용", description = "위험한 구역(zone)에 위치한 사람들에게 일괄 전송")
    @PostMapping("/zone")
    public ResponseEntity<Object> sendZoneMessage(@RequestBody FCMZoneRequest request) {
//        fcmService.sendMessage();
        fcmService.sendZoneSafety(
                request.getZoneId(),
                request.getDangerLevel(),
                TriggerType.MANUAL,
                LocalDateTime.now(),
                null
        );
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "설비 점검(maintain)알람 호출용", description = "설비와 점검자 ID를 받아 호출")
    @PostMapping("/equip")
    public ResponseEntity<Object> sendEquipoMessage(@RequestBody FCMEquipRequest request) {
//        fcmService.sendMessage();
        fcmService.sendEquipMaintain(request.getWorkerId(), request.getEquipId());
        return ResponseEntity.ok().build();
    }
}
