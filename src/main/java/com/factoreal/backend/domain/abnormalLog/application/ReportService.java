package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.response.DangerStatResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.GradeSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.MonthlyDetailResponse;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {
    private final AbnormalLogRepoService abnLogRepoService;


    /**
     * 이전 달의 리포트의 상세 정보를 열람하기 위한 서비스입니다.
     * 각 타입별 경고 횟수, 위험 횟수를 반환합니다.
     */
    public MonthlyDetailResponse getPrevMonthDetail(){

        List<AbnormalLog> logs = abnLogRepoService.findPreviewMonthLog();

        Map<TargetType, List<AbnormalLog>> byType =
                logs.stream().collect(Collectors.groupingBy(AbnormalLog::getTargetType));

        List<DangerStatResponse> stats = Arrays.stream(TargetType.values()).map(
                t -> {
                    List<AbnormalLog> list = byType.getOrDefault(t, List.of());
                    long warn   = list.stream().filter(l -> l.getDangerLevel() == 1).count();
                    long danger = list.stream().filter(l -> l.getDangerLevel() == 2).count();
                    return DangerStatResponse.builder()
                            .type(t.name()).warnCnt(warn).dangerCnt(danger).build();
                })
                .toList();

        String month = YearMonth.from(LocalDate.now().minusMonths(1)).toString(); // ex "2025-04"

        return MonthlyDetailResponse.builder().month(month).stats(stats).build();
    }

    /**
     * 이전달 리포트의 요약 내용을 보여줍니다.
     * 이전달의 각 파트별 등급을 반환합니다.
     */
    /* 2) 전달 한 달치 A/B/C 등급 요약 */
    public List<GradeSummaryResponse> getPrevMonthGrade() {
        YearMonth ym = YearMonth.from(LocalDate.now().minusMonths(1)); // ex 2025-04
        String monthStr = ym.toString();        // "2025-04"

        List<DangerStatResponse> stats = getPrevMonthDetail().getStats();
        return stats.stream()
                .map(s -> GradeSummaryResponse.builder()
                        .month(monthStr)
                        .type(s.getType())
                        .warnCnt(s.getWarnCnt())
                        .dangerCnt(s.getDangerCnt())
                        .grade(calcGrade(s.getWarnCnt(), s.getDangerCnt()))
                        .build())
                .toList();
    }

    private String calcGrade(long warn, long danger) {
        long effective = danger + warn / 5;      // 경고 5회 = 위험 1회 환산
        if (effective <= 2)  return "A";
        if (effective <= 5)  return "B";
        return "C";
    }

}
