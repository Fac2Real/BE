package com.factoreal.backend.domain.abnormalLog.api;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.request.AbnormalPagingRequest;
import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/abnormal")
@RequiredArgsConstructor
public class AbnormalController {

    private final AbnormalLogService abnormalLogService;

    // 전체 로그 조회
    @GetMapping
    public Page<AbnormalLogResponse> getAllAbnormalLogs(@ModelAttribute AbnormalPagingRequest pagingDto) {
        return abnormalLogService.findAllAbnormalLogs(pagingDto);
    }

    @GetMapping("/unread")
    public Page<AbnormalLogResponse> getAllAbnormalLogsUnRead(@ModelAttribute AbnormalPagingRequest pagingDto) {
        return abnormalLogService.findAllAbnormalLogsUnRead(pagingDto);
    }

    // 특정 이상 유형으로 필터링
    @GetMapping("/type/{abnormalType}")
    public Page<AbnormalLogResponse> getAbnormalLogsByType(
            @ModelAttribute AbnormalPagingRequest pagingDto,
            @PathVariable String abnormalType) {
        return abnormalLogService.findAbnormalLogsByAbnormalType(pagingDto, abnormalType);
    }

    // 특정 타겟 ID로 필터링
    @GetMapping("/target/{targetType}/{targetId}")
    public Page<AbnormalLogResponse> getAbnormalLogsByTarget(
            @ModelAttribute AbnormalPagingRequest pagingDto,
            @PathVariable String targetType,
            @PathVariable String targetId) {
        return abnormalLogService.findAbnormalLogsByTargetId(pagingDto, targetType, targetId);
    }

    // 알람 읽음 처리
    @PostMapping("/{abnormalId}/read")
    public ResponseEntity<Void> markAlarmAsRead(@PathVariable Long abnormalId) {
        boolean success = abnormalLogService.readCheck(abnormalId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // 읽지 않은 알람 개수 반환 -> websocket으로 전부 보내주기
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadAlarmCount() {
        Long count = abnormalLogService.readRequired();
        return ResponseEntity.ok(count);
    }
}
