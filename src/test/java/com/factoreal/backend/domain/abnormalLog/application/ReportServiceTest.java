package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.response.DangerStatResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.GradeSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.MonthlyDetailResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.MonthlyGradeSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse.*;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportGraphResponse.BarResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportGraphResponse.GraphSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.factoreal.backend.domain.controlLog.application.ControlLogRepoService;
import com.factoreal.backend.domain.equip.dto.response.EquipDetailResponse;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.worker.application.WorkerRepoService;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.application.ZoneService;
import com.factoreal.backend.domain.zone.dto.response.ZoneDetailResponse;
import com.factoreal.backend.domain.zone.entity.Zone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    AbnormalLogRepoService abnLogRepoService;   // 의존성 목킹

    @InjectMocks
    ReportService reportService;                // 테스트 대상


    /* ── ① 상세 리포트 페이지를 위한 목업 레포들 ─────────────────────────────── */
    @Mock
    ControlLogRepoService ctlRepo;
    @Mock
    ZoneService zoneSvc;
    @Mock
    SensorRepoService sensorRepo;
    @Mock
    WorkerRepoService workerRepo;

    /**
     * 전달 한 달치 상세 통계
     */
    @Test
    @DisplayName("getPrevMonth() - 경고/위험 건수 올바르게 집계")
    void getPrevMonth() {
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
                .type("작업자")
                .build();
        DangerStatResponse sensorStatResponse = DangerStatResponse
                .builder()
                .dangerCnt(2)
                .warnCnt(0)
                .type("환경")
                .build();
        DangerStatResponse EquipStatResponse = DangerStatResponse
                .builder()
                .dangerCnt(0)
                .warnCnt(1)
                .type("설비")
                .build();

        List<DangerStatResponse> resAnswer = List.of(sensorStatResponse, workerStatResponse, EquipStatResponse);
        when(abnLogRepoService.findPreview30daysLog()).thenReturn(fakeLogs);
        List<AbnormalLog> logs = abnLogRepoService.findPreview30daysLog();
        Map<TargetType, List<AbnormalLog>> byType =
                logs.stream().collect(Collectors.groupingBy(AbnormalLog::getTargetType));

        List<DangerStatResponse> stats = Arrays.stream(TargetType.values()).map(
                        t -> {
                            List<AbnormalLog> list = byType.getOrDefault(t, List.of());
                            long warn = list.stream().filter(l -> l.getDangerLevel() == 1).count();
                            long danger = list.stream().filter(l -> l.getDangerLevel() == 2).count();
                            return DangerStatResponse.builder()
                                    .type(koreanName(t)).warnCnt(warn).dangerCnt(danger).build();
                        })
                .toList();

        String month = YearMonth.from(LocalDate.now().minusMonths(1)).toString(); // ex "2025-04"

        // when
        MonthlyDetailResponse res = MonthlyDetailResponse.builder().period(month).stats(stats).build();

        assertThat(res.getStats()).isEqualTo(resAnswer);

        System.out.println(res);
    }

    private String koreanName(TargetType t) {
        return switch (t) {
            case Worker -> "작업자";
            case Sensor -> "환경";
            case Equip -> "설비";
        };
    }

    /**
     * 전달 한 달치 등급 요약
     */
    @Test
    @DisplayName("getPrevMonthGrade() - 등급 계산 로직 검증")
    void getPrevMonthGrade() {
        // given : Worker 위험 3 → effective 3 → Grade B
        List<AbnormalLog> fakeLogs = List.of(
                createLog(TargetType.Worker, 2),  //Worker 경고 (B)
                createLog(TargetType.Worker, 2),
                createLog(TargetType.Worker, 2),  
                createLog(TargetType.Sensor, 1),  // Sensor 경고 1 → effective 0 (A)
                createLog(TargetType.Equip, 2),    // Equip 경고 6개 → effective 5 (C)
                createLog(TargetType.Equip, 2),
                createLog(TargetType.Equip, 2),
                createLog(TargetType.Equip, 2),
                createLog(TargetType.Equip, 2),
                createLog(TargetType.Equip, 2)

        );
        when(abnLogRepoService.findPreview30daysLog()).thenReturn(fakeLogs);

        // when
        MonthlyGradeSummaryResponse response = reportService.getPrevMonthGrade();

        List<GradeSummaryResponse> grades = response.getAbnormalInfos();

        // then
        GradeSummaryResponse worker = grades.stream()
                .filter(g -> g.getType().equals("작업자")).findFirst().orElseThrow();
        assertThat(worker.getGrade()).isEqualTo("B");

        GradeSummaryResponse sensor = grades.stream()
                .filter(g -> g.getType().equals("환경")).findFirst().orElseThrow();
        assertThat(sensor.getGrade()).isEqualTo("A");

        GradeSummaryResponse equip = grades.stream()
                .filter(g -> g.getType().equals("설비")).findFirst().orElseThrow();
        assertThat(equip.getGrade()).isEqualTo("C");
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


    /* ── ② 테스트 대상 ──────────────────────────── */

    /* ── ③ 공통 헬퍼 ────────────────────────────── */
    private AbnormalLog log(long id, TargetType tp, String tgtId,
                            String zoneId, String zoneName, int danger) {
        return AbnormalLog.builder()
                .id(id).targetType(tp).targetId(tgtId)
                .zone(Zone.builder()
                        .zoneId(zoneId)
                        .zoneName(zoneName)
                        .build())
                .dangerLevel(danger)
                .detectedAt(LocalDateTime.now())
                .build();
    }

    private ZoneDetailResponse zone(String id, String name,
                                    List<EquipDetailResponse> eqlist) {
        return ZoneDetailResponse.builder()
                .zoneId(id).zoneName(name)
                .envList(List.of())  // 간단히 비워 둠
                .equipList(eqlist)
                .build();
    }

    private EquipDetailResponse equip(String eid, String name) {
        return EquipDetailResponse.builder()
                .equipId(eid).equipName(name)
                .facSensor(List.of())  // 필요 시 센서 넣기
                .build();
    }

    /* ── ④ 실제 테스트 ─────────────────────────── */
    @Test
    @DisplayName("buildLast30DaysReport() : 최근 30일에 대한 이상치 로그의 반환과 갯수를 잘 파악하는지 테스트")
    void buildLast30DaysReport_basicCounts() throws JsonProcessingException {
        /* 준비 : Zone 1개, Equip 1대  */
        ZoneDetailResponse zm = zone("Z1", "생산 A",
                List.of(equip("E1", "로봇암1")));
        when(zoneSvc.getZoneItems()).thenReturn(List.of(zm));

        /* 준비 : AbnormalLog – 1)Sensor 2)Worker 3)Equip */
        AbnormalLog lg1 = log(1, TargetType.Sensor, "S-Tmp-1", "Z1","Z-1", 1);
        AbnormalLog lg2 = log(2, TargetType.Worker, "W-100", "Z1", "Z-1" ,2);
        AbnormalLog lg3 = log(3, TargetType.Equip, "S-E1-tmp", "Z1", "Z-1", 1);
        when(abnLogRepoService.findPreview30daysLog()).thenReturn(List.of(lg1, lg2, lg3));

        /* 준비 : ControlLog(없어도 됨) */
        when(ctlRepo.getControlLogs(Mockito.anyList())).thenReturn(Map.of());

        /* 준비 : Sensor→Equip 매핑 */
        when(sensorRepo.sensorIdToEquipId(Mockito.anyList()))
                .thenReturn(Map.of("S-E1-tmp", "E1"));

        /* 준비 : Worker 상세 */
        Worker w100 = Worker.builder()
                .workerId("W-100").name("홍길동").phoneNumber("+82101").build();
        when(workerRepo.findWorkersMap(anyList()))
                .thenAnswer(inv -> {
                    // 실제로 넘긴 id 목록
                    List<String> ids = inv.getArgument(0);
                    // ids 안에 존재하는 것만 골라서 Map 으로 돌려준다
                    return ids.stream()
                            .filter("W-100"::equals)
                            .collect(Collectors.toMap(id -> id, id -> w100));
                });

        /* 실행 */
        PeriodDetailReportResponse rpt = reportService.buildLast30DaysReport();

        System.out.println();
        /* 검증 */
        assertThat(rpt.getZones()).hasSize(1);
        ZoneBlockResponse zb = rpt.getZones().get(0);

        assertThat(zb.getEnvCnt()).isEqualTo(1);  // Sensor 1
        assertThat(zb.getWorkerCnt()).isEqualTo(1);  // Worker 1
        assertThat(zb.getFacCnt()).isEqualTo(1);  // Equip 1
        assertThat(zb.getTotalCnt()).isEqualTo(3);

        EquipBlockResponse eb = zb.getEquips().get(0);
        assertThat(eb.getEquipId()).isEqualTo("E1");
        assertThat(eb.getFacCnt()).isEqualTo(1);

        // ── 반환되는 객체 구조 확인 ───────────────────
        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println(om.writeValueAsString(rpt));
    }

    @Test
    @DisplayName("buildLastMonthsReport() : 최근 30일에 대한 이상치 로그의 반환과 갯수를 잘 파악하는지 테스트")
    void buildLastMonthsReport_basicCounts() throws JsonProcessingException {
        /* 준비 : Zone 1개, Equip 1대  */
        ZoneDetailResponse zm = zone("Z1", "생산 A",
                List.of(equip("E1", "로봇암1")));
        when(zoneSvc.getZoneItems()).thenReturn(List.of(zm));

        /* 준비 : AbnormalLog – 1)Sensor 2)Worker 3)Equip */
        AbnormalLog lg1 = log(1, TargetType.Sensor, "S-Tmp-1", "Z1", "Z-1", 1);
        AbnormalLog lg2 = log(2, TargetType.Worker, "W-100", "Z1", "Z-1", 2);
        AbnormalLog lg3 = log(3, TargetType.Equip, "S-E1-tmp", "Z1", "Z-1", 1);
        when(abnLogRepoService.findPreviousMonthLogs()).thenReturn(List.of(lg1, lg2, lg3));

        /* 준비 : ControlLog(없어도 됨) */
        when(ctlRepo.getControlLogs(Mockito.anyList())).thenReturn(Map.of());

        /* 준비 : Sensor→Equip 매핑 */
        when(sensorRepo.sensorIdToEquipId(Mockito.anyList()))
                .thenReturn(Map.of("S-E1-tmp", "E1"));

        /* 준비 : Worker 상세 */
        Worker w100 = Worker.builder()
                .workerId("W-100").name("홍길동").phoneNumber("+82101").build();
        when(workerRepo.findWorkersMap(anyList()))
                .thenAnswer(inv -> {
                    // 실제로 넘긴 id 목록
                    List<String> ids = inv.getArgument(0);
                    // ids 안에 존재하는 것만 골라서 Map 으로 돌려준다
                    return ids.stream()
                            .filter("W-100"::equals)
                            .collect(Collectors.toMap(id -> id, id -> w100));
                });

        /* 실행 */
        PeriodDetailReportResponse rpt = reportService.buildLastMonthReport();

        System.out.println();
        /* 검증 */
        assertThat(rpt.getZones()).hasSize(1);
        ZoneBlockResponse zb = rpt.getZones().get(0);

        assertThat(zb.getEnvCnt()).isEqualTo(1);  // Sensor 1
        assertThat(zb.getWorkerCnt()).isEqualTo(1);  // Worker 1
        assertThat(zb.getFacCnt()).isEqualTo(1);  // Equip 1
        assertThat(zb.getTotalCnt()).isEqualTo(3);

        EquipBlockResponse eb = zb.getEquips().get(0);
        assertThat(eb.getEquipId()).isEqualTo("E1");
        assertThat(eb.getFacCnt()).isEqualTo(1);

        // ── 반환되는 객체 구조 확인 ───────────────────
        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println(om.writeValueAsString(rpt));
    }

    /* buildZoneBlock() 을 따로 검증하고 싶다면
       - ZoneDetailResponse 1개와 그 Zone 에 속하는 로그 리스트만 넘긴다
       - private → package 로 열어두거나 @Testable 로 감싸서 호출 */
    @Test
    void toDetail_mapping() throws Exception {
        AbnormalLog src = log(10, TargetType.Sensor, "S-1", "Z1", "Z-1", 2);
        ControlLog ctl = ControlLog.builder()
                .abnormalLog(src).controlType("에어컨")
                .controlVal(25D).controlStat(1)
                .executedAt(LocalDateTime.now())
                .build();

        // 리플렉션으로 private 메서드 호출 예시
        Method m = ReflectionUtils
                .findMethod(ReportService.class, "toDetail", AbnormalLog.class, ControlLog.class);

        assertThat(m).isNotNull();              // ① null-check
        ReflectionUtils.makeAccessible(m);      // ② Spring util – null 안전
        m.setAccessible(true);

        AbnDetailResponse dt = (AbnDetailResponse) m.invoke(reportService, src, ctl);

        assertThat(dt.getAbnormalId()).isEqualTo(10L);
        assertThat(dt.getControl().getControlType()).isEqualTo("에어컨");
    }

    /* 테스트 케이스 확장 */
    /* ========== Worker 정보가 조회되지 않을 때 name·phone 이 null 인지 검증 ========== */
    @Test
    @DisplayName("buildLast30DaysReport : workerMap 에 없는 작업자 로그 → name, phone = null")
    void buildReport_workerUnknown_setsNullFields() {

        /* ① Zone · Equip 세팅 */
        ZoneDetailResponse zm = zone("Z1", "포장실",
                List.of(equip("E1", "로봇암1")));
        when(zoneSvc.getZoneItems()).thenReturn(List.of(zm));

        /* ② AbnormalLog – Worker 로그만 하나 (workerId = W-999) */
        AbnormalLog wLog = log(10, TargetType.Worker, "W-999", "Z1", "Z-1", 1);
        when(abnLogRepoService.findPreview30daysLog()).thenReturn(List.of(wLog));

        /* ③ ControlLog, Sensor→Equip 매핑은 없어도 OK */
        when(ctlRepo.getControlLogs(anyList())).thenReturn(Map.of());
        when(sensorRepo.sensorIdToEquipId(anyList())).thenReturn(Map.of());

        /* ④ WorkerRepo 에서 “빈 Map” 반환 → W-999 정보를 찾지 못하는 상황 */
        when(workerRepo.findWorkersMap(anyList())).thenReturn(Map.of());

        /* ⑤ 실행 */
        PeriodDetailReportResponse rpt = reportService.buildLast30DaysReport();

        /* ⑥ 검증 */
        ZoneBlockResponse zb = rpt.getZones().get(0);
        assertThat(zb.getWorkerCnt()).isEqualTo(1);

        WorkerBlockResponse wb = zb.getWorkers().get(0);
        assertThat(wb.getWorkerId()).isEqualTo("W-999");
        assertThat(wb.getName()).isNull();          // ← w == null 분기
        assertThat(wb.getPhone()).isNull();
    }
    @Test
    @DisplayName("buildLast30DaysGraph : 집계·정렬·기간 문자열 검증")
    void buildLast30DaysGraph_countsAndOrder() {
        /* ── 1. 테스트용 날짜 계산 ───────────────── */
        LocalDate today      = LocalDate.now();
        LocalDate yesterday  = today.minusDays(1);
        LocalDate d1         = yesterday.minusDays(5);   // 그래프용 날짜①
        LocalDate d2         = yesterday.minusDays(10);  // 그래프용 날짜②

        /* ── 2. AbnormalLog 4건 준비 ──────────────── */
        AbnormalLog s1 = log(1, TargetType.Sensor, "S-1", "Z-A", "Z-A", 1); // SENSOR
        s1.setDetectedAt(d1.atTime(12, 0));

        AbnormalLog s2 = log(2, TargetType.Sensor, "S-2", "Z-A", "Z-A",2); // SENSOR
        s2.setDetectedAt(d1.atTime(13, 0));

        AbnormalLog e1 = log(3, TargetType.Equip , "S-3", "Z-B","Z-B", 1); // EQUIP
        e1.setDetectedAt(d2.atTime( 9, 0));

        AbnormalLog w1 = log(4, TargetType.Worker, "W-1", "Z-A", "Z-A", 1); // WORKER
        w1.setDetectedAt(d2.atTime(10, 0));

        List<AbnormalLog> fake = List.of(s1, s2, e1, w1);

        when(abnLogRepoService.findByDetectedAtBetweenAndDangerLevelIn(
                any(), any(), Mockito.eq(List.of(1, 2))))
                .thenReturn(fake);

        /* ── 3. 실행 ──────────────────────────────── */
        GraphSummaryResponse res = reportService.buildLast30DaysGraph();

        /* ── 4-1. 기간 문자열 검증 ────────────────── */
        String expPeriod = yesterday.minusDays(30).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                + " ~ " +
                yesterday.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        assertThat(res.getPeriod()).isEqualTo(expPeriod);

        /* ── 4-2. typeStats (카운트 내림차순) ──────── */
        List<BarResponse> typeStats = res.getTypeStats();
        assertThat(typeStats).hasSize(3);                      // SENSOR/EQUIP/WORKER 3종
        assertThat(typeStats.get(0)).satisfies(b -> {         // 1) SENSOR 2건
            assertThat(b.getLabel()).isEqualTo("Sensor");
            assertThat(b.getCnt()).isEqualTo(2);
        });
        Map<String, Long> typeMap = typeStats.stream()
                .collect(Collectors.toMap(BarResponse::getLabel, BarResponse::getCnt));
        assertThat(typeMap).containsEntry("Equip",   1L)
                .containsEntry("Worker",  1L);

        /* ── 4-3. dateStats (‘MM-dd’ 오름차순) ─────── */
        List<BarResponse> dateStats = res.getDateStats();
        DateTimeFormatter mmdd = DateTimeFormatter.ofPattern("MM-dd");
        assertThat(dateStats).extracting(BarResponse::getLabel)
                .containsExactly(mmdd.format(d2), mmdd.format(d1));    // 오름차순

        Map<String, Long> dateMap = dateStats.stream()
                .collect(Collectors.toMap(BarResponse::getLabel, BarResponse::getCnt));
        assertThat(dateMap.get(mmdd.format(d1))).isEqualTo(2L);
        assertThat(dateMap.get(mmdd.format(d2))).isEqualTo(2L);

        /* ── 4-4. zoneStats (카운트 내림차순) ──────── */
        List<BarResponse> zoneStats = res.getZoneStats();
        assertThat(zoneStats.get(0)).satisfies(b -> {
            assertThat(b.getLabel()).isEqualTo("Z-A");
            assertThat(b.getCnt()).isEqualTo(3L);              // Z-A 3건
        });
        assertThat(zoneStats.get(1)).satisfies(b -> {
            assertThat(b.getLabel()).isEqualTo("Z-B");
            assertThat(b.getCnt()).isEqualTo(1L);
        });
    }

}