package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.response.DangerStatResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.GradeSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.MonthlyDetailResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.MonthlyGradeSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse.PeriodDetailReport;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.zone.application.ZoneService;
import com.factoreal.backend.domain.zone.dto.response.ZoneDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {
    private final AbnormalLogRepoService abnLogRepoService;
    private final ZoneService zoneService;
    private final AbnormalLogRepoService abnormalLogRepoService;


    /**
     * 오늘로부터 한달 이전의 리포트의 상세 정보를 열람하기 위한 서비스입니다.
     * 각 타입별 경고 횟수, 위험 횟수를 반환합니다.
     */
    public MonthlyDetailResponse getPrevMonthDetail(){

        List<AbnormalLog> logs = abnLogRepoService.findPreview30daysLog();

        Map<TargetType, List<AbnormalLog>> byType =
                logs.stream().collect(Collectors.groupingBy(AbnormalLog::getTargetType));

        List<DangerStatResponse> stats = Arrays.stream(TargetType.values()).map(
                t -> {
                    List<AbnormalLog> list = byType.getOrDefault(t, List.of());
                    long warn   = list.stream().filter(l -> l.getDangerLevel() == 1).count();
                    long danger = list.stream().filter(l -> l.getDangerLevel() == 2).count();
                    return DangerStatResponse.builder()
                            .type(koreanName(t)).warnCnt(warn).dangerCnt(danger).build();
                })
                .toList();

        LocalDate end   = LocalDate.now().minusDays(1);;          // 오늘
        LocalDate start = end.minusDays(30);        // 30일 전

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        // 2️⃣ 문자열 생성  →  2025.04.30 ~ 2025.05.30
        String monthPeriod = fmt.format(start) + " ~ " + fmt.format(end);

        return MonthlyDetailResponse.builder().period(monthPeriod).stats(stats).build();
    }

    private String koreanName(TargetType t) {
        return switch (t) {
            case Worker  -> "작업자";
            case Sensor  -> "환경";
            case Equip   -> "설비";
        };
    }
    /**
     * 이전달 리포트의 요약 내용을 보여줍니다.
     * 이전달의 각 파트별 등급을 반환합니다.
     */
    /* 2) 전달 한 달치 A/B/C 등급 요약 */
    public MonthlyGradeSummaryResponse getPrevMonthGrade() {
        LocalDate end   = LocalDate.now().minusDays(1);          // 오늘
        LocalDate start = end.minusDays(30);        // 30일 전

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        // 2️⃣ 문자열 생성  →  2025.04.30 ~ 2025.05.30
        String monthPeriod = fmt.format(start) + " ~ " + fmt.format(end);

        List<DangerStatResponse> stats = getPrevMonthDetail().getStats();
        List<GradeSummaryResponse> summaryList = stats.stream()
                .map(s -> GradeSummaryResponse.builder()
//                        .latest30days(monthPeriod)
                        .type(s.getType())
                        .warnCnt(s.getWarnCnt())
                        .dangerCnt(s.getDangerCnt())
                        .grade(calcGrade(s.getWarnCnt(), s.getDangerCnt()))
                        .build())
                .toList();

        return MonthlyGradeSummaryResponse.builder()
                .period(monthPeriod)
                .abnormalInfos(summaryList)
                .build();
    }

    private String calcGrade(long warn, long danger) {
        long effective = danger + warn / 5;      // 경고 5회 = 위험 1회 환산
        if (effective <= 2)  return "A";
        if (effective <= 5)  return "B";
        return "C";
    }

}
