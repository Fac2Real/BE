package com.factoreal.backend.domain.abnormalLog.api;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.request.AbnormalPagingRequest;
import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/abnormal")
@RequiredArgsConstructor
@Tag(name = "상태 로그 조회 API", description = "작업자/환경/설비 이상의 로그를 조회하는 API")
public class AbnormalController {
    private final AbnormalLogService abnormalLogService;

    // 전체 로그 조회
    @GetMapping
    @Operation(summary = "전체 로그 조회", description = "페이징 인자를 기준으로 로그를 요정 개수 만큼 반환해줍니다.")
    public Page<AbnormalLogResponse> getAllAbnormalLogs(@ModelAttribute AbnormalPagingRequest pagingDto) {
        return abnormalLogService.findAllAbnormalLogs(pagingDto);
    }

    @GetMapping("/unread")
    @Operation(summary = "미확인 로그 조회", description = "페이징 인자를 기준으로 읽지 않은 로그를 요정 개수 만큼 반환해줍니다.")
    public Page<AbnormalLogResponse> getAllAbnormalLogsUnRead(@ModelAttribute AbnormalPagingRequest pagingDto) {
        return abnormalLogService.findAllAbnormalLogsUnRead(pagingDto);
    }

    // 특정 이상 유형으로 필터링
    @GetMapping("/type/{abnormalType}")
    @Operation(summary = "위험별 로그 조회", description = "페이징 인자를 기준으로 위험별 로그를 요정 개수 만큼 반환해줍니다.")
    public Page<AbnormalLogResponse> getAbnormalLogsByType(
            @ModelAttribute AbnormalPagingRequest pagingDto,
            @PathVariable String abnormalType) {
        return abnormalLogService.findAbnormalLogsByAbnormalType(pagingDto, abnormalType);
    }

    // 특정 타겟 ID로 필터링
    @GetMapping("/target/{targetType}/{targetId}")
    @Operation(summary = "유형별 로그 조회", description = "페이징 인자를 기준으로 유형별 로그를 요정 개수 만큼 반환해줍니다.")
    public Page<AbnormalLogResponse> getAbnormalLogsByTarget(
            @ModelAttribute AbnormalPagingRequest pagingDto,
            @PathVariable TargetType targetType,
            @PathVariable String targetId) {
        return abnormalLogService.findAbnormalLogsByTargetId(pagingDto, targetType, targetId);
    }

    // 알람 읽음 처리
    @PostMapping("/{abnormalId}/read")
    @Operation(summary = "로그 읽음 처리", description = "abnormalId와 일치하는 로그를 읽음 처리합니다.")
    public ResponseEntity<Void> markAlarmAsRead(@PathVariable Long abnormalId) {
        boolean success = abnormalLogService.readCheck(abnormalId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    // 웹 페이지 첫 렌더링 시 호출되는 api
    // 읽지 않은 알람 개수 반환 -> websocket으로 전부 보내주기
    @GetMapping("/unread-count")
    @Operation(summary = "미확인 로그 개수 조회", description = "미확인 로그 개수를 반환합니다. 페이지이 첫 렌더링 시(웹소켓으로 정보를 받기전) 호출합니다.")
    public ResponseEntity<Long> getUnreadAlarmCount() {
        Long count = abnormalLogService.readRequired();
        return ResponseEntity.ok(count);
    }
}
