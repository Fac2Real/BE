package com.factoreal.backend.domain.abnormalLog.api;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.application.ReportMailService;
import com.factoreal.backend.domain.abnormalLog.application.ReportService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.request.AbnormalPagingRequest;
import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.MonthlyGradeSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse.PeriodDetailReportResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportGraphResponse.GraphSummaryResponse;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/abnormal")
@RequiredArgsConstructor
@Tag(name = "상태 로그 조회 API", description = "작업자/환경/설비 이상의 로그를 조회하는 API")
public class AbnormalController {
    private final AbnormalLogService abnormalLogService;
    private final WebSocketSender webSocketSender;
    private final AbnormalLogRepoService abnormalLogRepoService;
    private final ReportService reportService;
    private final ReportMailService reportMailService;

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
        Long count = abnormalLogRepoService.countByIsReadFalse();
        webSocketSender.sendUnreadCount(count);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // 웹 페이지 첫 렌더링 시 호출되는 api
    // 읽지 않은 알람 개수 반환 -> websocket으로 전부 보내주기
    @GetMapping("/unread-count")
    @Operation(summary = "미확인 로그 개수 조회", description = "미확인 로그 개수를 반환합니다. 페이지이 첫 렌더링 시(웹소켓으로 정보를 받기전) 호출합니다.")
    public ResponseEntity<Long> getUnreadAlarmCount() {
        Long count = abnormalLogRepoService.countByIsReadFalse();
        webSocketSender.sendUnreadCount(count);
        return ResponseEntity.ok(count);
    }

//    @Operation(summary = "이전 한달치에 대한 리포트 조회", description = "이전 한달 모니터링 레포트를 조회합니다.")
//    @GetMapping("/report")
//    public ResponseEntity<MonthlyDetailResponse> getPrevReport() {
//        return ResponseEntity.ok(reportService.getPrevMonthDetail());
//    }

    @GetMapping("/report")
    @Operation(summary = "이전 한달치에 대한 리포트 조회", description = "이전 한달 모니터링 레포트를 조회합니다.")
    public ResponseEntity<MonthlyGradeSummaryResponse> getPrevReport() {
        log.info("리포트 조회 작동");
        return ResponseEntity.ok(reportService.getPrevMonthGrade());
    }

    @GetMapping("/detail-report")
    @Operation(summary = "이전 한달치 상세 로그 데이터 조회", description = "이전 한달치 데이터의 로그를 조회합니다.")
    public ResponseEntity<PeriodDetailReportResponse> detail() {
        return ResponseEntity.ok(reportService.buildLast30DaysReport());
    }

    @Scheduled(cron = "0 0 17 * * *", zone = "Asia/Seoul")
    @GetMapping("/send-report")
    @Operation(summary = "공간 담당자들에게 이상치 리포트를 전송하는 테스트용 api", description = "메일 전송 체크를 위한 api")
    public ResponseEntity<Void> sendReport() throws Exception {
        reportMailService.sendMonthlyDetailReports();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/graph-report")
    @Operation(summary = "지난 30일 그래프용 이상치 횟수 집계", description = "이상치 횟수를 상황별로 보기 위한 api")
    public GraphSummaryResponse getSummary() {
        return reportService.buildLast30DaysGraph();
    }

}
