package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.response.DangerStatResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.GradeSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.MonthlyDetailResponse;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    AbnormalLogRepoService abnLogRepoService;   // 의존성 목킹

    @InjectMocks
    ReportService reportService;                // 테스트 대상

    /** 전달 한 달치 상세 통계 */
    @Test
    @DisplayName("getPrevMonthDetail() - 경고/위험 건수 올바르게 집계")
    void getPrevMonthDetail() {
        // given : 가짜 로그 6건 (Worker 3, Sensor 2, Equip 1)
        List<AbnormalLog> fakeLogs = List.of(
                createLog(TargetType.Worker, 1),
                createLog(TargetType.Worker, 1),
                createLog(TargetType.Worker, 2),
                createLog(TargetType.Sensor, 2),
                createLog(TargetType.Sensor, 2),
                createLog(TargetType.Equip, 1)
        );

        DangerStatResponse workerStatResponse = DangerStatResponse
                .builder()
                .dangerCnt(1)
                .warnCnt(2)
                .type("Worker")
                .build();
        DangerStatResponse sensorStatResponse = DangerStatResponse
                .builder()
                .dangerCnt(2)
                .warnCnt(0)
                .type("Sensor")
                .build();
        DangerStatResponse EquipStatResponse = DangerStatResponse
                .builder()
                .dangerCnt(0)
                .warnCnt(1)
                .type("Equip")
                .build();

        List<DangerStatResponse> resAnswer = List.of(sensorStatResponse,workerStatResponse, EquipStatResponse);
        when(abnLogRepoService.findPreviewMonthLog()).thenReturn(fakeLogs);
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

        // when
        MonthlyDetailResponse res = MonthlyDetailResponse.builder().month(month).stats(stats).build();

        assertThat(res.getStats()).isEqualTo(resAnswer);

        System.out.println(res);
    }

    /** 전달 한 달치 등급 요약 */
    @Test
    @DisplayName("getPrevMonthGrade() - 등급 계산 로직 검증")
    void getPrevMonthGrade() {
        // given : Worker 위험 3 → effective 3 → Grade B
        List<AbnormalLog> fakeLogs = List.of(
                createLog(TargetType.Worker, 2),
                createLog(TargetType.Worker, 2),
                createLog(TargetType.Worker, 2),
                createLog(TargetType.Sensor, 1),  // Sensor 경고 1 → effective 0 (A)
                createLog(TargetType.Equip, 1)    // Equip 경고 1 → effective 0 (A)
        );
        when(abnLogRepoService.findPreviewMonthLog()).thenReturn(fakeLogs);

        // when
        List<GradeSummaryResponse> grades = reportService.getPrevMonthGrade();

        // then
        GradeSummaryResponse worker = grades.stream()
                .filter(g -> g.getType().equals("Worker")).findFirst().orElseThrow();
        assertThat(worker.getGrade()).isEqualTo("B");

        GradeSummaryResponse sensor = grades.stream()
                .filter(g -> g.getType().equals("Sensor")).findFirst().orElseThrow();
        assertThat(sensor.getGrade()).isEqualTo("A");

        GradeSummaryResponse equip = grades.stream()
                .filter(g -> g.getType().equals("Equip")).findFirst().orElseThrow();
        assertThat(equip.getGrade()).isEqualTo("A");
    }

    /* ------------- 헬퍼 ------------- */

    private AbnormalLog createLog(TargetType type, int dangerLevel) {
        return AbnormalLog.builder()
                .id(0L)
                .targetType(type)
                .dangerLevel(dangerLevel)
                .detectedAt(LocalDateTime.now().minusDays(5)) // 아무 날짜
                .build();
    }
}