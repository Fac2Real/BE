package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.response.DangerStatResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.GradeSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.MonthlyDetailResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.MonthlyGradeSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse.*;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportGraphResponse.Bar;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportGraphResponse.GraphSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.factoreal.backend.domain.controlLog.service.ControlLogRepoService;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.worker.application.WorkerRepoService;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.application.ZoneService;
import com.factoreal.backend.domain.zone.dto.response.ZoneDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {
    private final AbnormalLogRepoService abnLogRepoService;
    private final ZoneService zoneService;
    private final AbnormalLogRepoService abnormalLogRepoService;
    private final ControlLogRepoService controlLogRepoService;
    private final SensorRepoService sensorRepoService;
    private final WorkerRepoService workerRepoService;


    /**
     * 오늘로부터 한달 이전의 리포트의 상세 정보를 열람하기 위한 서비스입니다.
     * 각 타입별 경고 횟수, 위험 횟수를 반환합니다.
     */
    public MonthlyDetailResponse getPrevMonth(){

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

        LocalDate end   = LocalDate.now().minusDays(1);          // 오늘
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

        List<DangerStatResponse> stats = getPrevMonth().getStats();
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

    /**
     * 전날 기준 최근 30일 이상치 조회
     */
    public PeriodDetailReport buildLast30DaysReport() {
        LocalDateTime end = LocalDateTime.now().minusDays(1);
        LocalDateTime start = end.minusDays(30);

        List<AbnormalLog> logs = abnormalLogRepoService.findPreview30daysLog();

        return buildPeriodDetailReport(start, end, logs);
    }

    /**
     * 이전달 이상치 로그 조회
     */
    public PeriodDetailReport buildLastMonthReport() {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDay  = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

        LocalDateTime start = firstDay.atStartOfDay();
        LocalDateTime end   = lastDay.atTime(LocalTime.MAX);

        List<AbnormalLog> logs = abnormalLogRepoService.findPreviousMonthLogs();

        return buildPeriodDetailReport(start, end, logs);
    }

    private PeriodDetailReport buildPeriodDetailReport(LocalDateTime start, LocalDateTime end, List<AbnormalLog> logs) {
        List<ZoneDetailResponse> zoneMeta = zoneService.getZoneItems();

        List<Long> abnIds = logs.stream()
                .map(AbnormalLog::getId)
                .toList();

        Map<Long, ControlLog> ctlMap = controlLogRepoService.getControlLogs(abnIds);

        Map<String, String> sensorToEquip = sensorRepoService.sensorIdToEquipId(
                logs.stream()
                        .filter(l -> l.getTargetType() == TargetType.Equip)
                        .map(AbnormalLog::getTargetId)
                        .distinct()
                        .toList()
        );

        Map<String, Worker> workerMap = workerRepoService.findWorkersMap(
                logs.stream()
                        .filter(l -> l.getTargetType() == TargetType.Worker)
                        .map(AbnormalLog::getTargetId)
                        .distinct()
                        .toList()
        );

        List<ZoneBlock> zones = zoneMeta.stream()
                .map(zm -> buildZoneBlock(zm, logs, ctlMap, sensorToEquip, workerMap))
                .toList();

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        String period = f.format(start.toLocalDate()) + " ~ " + f.format(end.toLocalDate());

        return new PeriodDetailReport(period, zones);
    }

    private ZoneBlock buildZoneBlock(ZoneDetailResponse zm,
                                     List<AbnormalLog> allLogs,
                                     Map<Long,ControlLog> ctlMap,
                                     Map<String,String> sensorToEquip,
                                     Map<String, Worker> workerMap) {

        String zid = zm.getZoneId();
        List<AbnormalLog> zLogs = allLogs.stream()
                .filter(l -> l.getZone().getZoneId().equals(zid))
                .toList();

        // ① 환경
        List<AbnDetail> envAbn = zLogs.stream()
                .filter(l -> l.getTargetType()==TargetType.Sensor)
                .map(l -> toDetail(l, ctlMap.get(l.getId())))
                .sorted(Comparator.comparing(AbnDetail::getDetectedAt))
                .toList();

        // ② 작업자
        List<AbnDetail> workerAbn = zLogs.stream()
                .filter(l -> l.getTargetType()==TargetType.Worker)
                .map(l -> toDetail(l, ctlMap.get(l.getId())))
                .toList();

        // ③ 설비별
        Map<String,List<AbnormalLog>> byEquip = zLogs.stream()
                .filter(l -> l.getTargetType()==TargetType.Equip)
                .collect(Collectors.groupingBy(
                        l -> sensorToEquip.getOrDefault(l.getTargetId(),"UNKNOWN")
                ));

        List<EquipBlock> equipBlocks = zm.getEquipList().stream()
                .map(em -> {
                    List<AbnormalLog> eLogs = byEquip.getOrDefault(em.getEquipId(),List.of());
                    List<AbnDetail> fac = eLogs.stream()
                            .map(l -> toDetail(l, ctlMap.get(l.getId())))
                            .sorted(Comparator.comparing(AbnDetail::getDetectedAt))
                            .toList();
                    return EquipBlock.builder()
                            .equipId(em.getEquipId())
                            .equipName(em.getEquipName())
                            .facCnt(fac.size())
                            .facAbnormals(fac)
                            .build();
                })
                .toList();

        Map<String, List<AbnormalLog>> byWorker = zLogs.stream()
                .filter(l -> l.getTargetType() == TargetType.Worker)
                .collect(Collectors.groupingBy(AbnormalLog::getTargetId)); // key = workerId

        List<WorkerBlock> workerBlocks = byWorker.entrySet().stream()
                .map(e -> {
                    String wid = e.getKey();
                    Worker   w = workerMap.get(wid);        // null possible

                    List<AbnDetail> workerDetails = e.getValue().stream()
                            .map(l -> toDetail(l, ctlMap.get(l.getId())))
                            .sorted(Comparator.comparing(AbnDetail::getDetectedAt))
                            .toList();

                    return WorkerBlock.builder()
                            .workerId(wid)
                            .name(   w != null ? w.getName()         : null)
                            .phone(  w != null ? w.getPhoneNumber()  : null)
                            .workerCnt(workerDetails.size())
                            .workerAbnormals(workerDetails)
                            .build();
                })
                .toList();

        int envCnt    = envAbn.size();
        int workerCnt = workerAbn.size();
        int facCnt    = equipBlocks.stream().mapToInt(EquipBlock::getFacCnt).sum();
        int total     = envCnt + workerCnt + facCnt;

        return ZoneBlock.builder()
                .zoneId(zid)
                .zoneName(zm.getZoneName())
                .envCnt(envCnt)
                .workerCnt(workerCnt)
                .facCnt(facCnt)
                .totalCnt(total)
                .envAbnormals(envAbn)
                .workers(workerBlocks)
                .equips(equipBlocks)
                .build();
    }

    // ────────────────────────────────────────────────
    // 2-4. AbnormalLog + ControlLog → AbnDetail 로 변환하는 헬퍼
    // ────────────────────────────────────────────────
    private AbnDetail toDetail(AbnormalLog l, ControlLog c) {
        return new AbnDetail(
                l.getId(),
                l.getDangerLevel(),
                l.getAbnormalType(),
                l.getAbnVal(),
                l.getDetectedAt(),
                c == null
                        ? null
                        : new ControlInfo(
                        c.getExecutedAt(),
                        c.getControlType(),
                        c.getControlVal(),
                        c.getControlStat()
                )
        );
    }

    public GraphSummaryResponse buildLast30DaysGraph() {

        /* ① 기간 계산 : 어제~30일 전 */
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate startDay  = yesterday.minusDays(30);     // 총 30일

        LocalDateTime start = startDay.atStartOfDay();
        LocalDateTime end   = yesterday.atTime(LocalTime.MAX);

        /* ② 로그 조회 (dangerLevel 1 | 2 만) */
        List<AbnormalLog> logs = abnormalLogRepoService.findByDetectedAtBetweenAndDangerLevelIn(
                start, end, List.of(1, 2));

        /* ③ 그래프 1 : TargetType 별 ─ 항상 3개(SENSOR/EQUIP/WORKER) 보장 */
        Map<TargetType, Long> typeCntMap = logs.stream()
                .collect(Collectors.groupingBy(AbnormalLog::getTargetType, Collectors.counting()));

        /* ③ 그래프 1 : TargetType 별 */
        List<Bar> typeStats = Arrays.stream(TargetType.values())
                .map(tp -> new Bar(tp.name(), typeCntMap.getOrDefault(tp, 0L)))
                .sorted(Comparator.comparingLong(Bar::getCnt).reversed())   // 큰 순서
                .toList();

        /* ④ 그래프 2 : 날짜(월-일) 별 */
        DateTimeFormatter mmdd = DateTimeFormatter.ofPattern("MM-dd");
        List<Bar> dateStats = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getDetectedAt().toLocalDate(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new Bar(mmdd.format(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(Bar::getLabel))           // x축 순서 보장
                .toList();

        /* ⑤ 그래프 3 : Zone 별 */
        List<Bar> zoneStats = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getZone().getZoneName(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new Bar(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(Bar::getCnt).reversed())
                .toList();

        /* ⑥ 기간 문자열 */
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        String period = f.format(startDay) + " ~ " + f.format(yesterday);

        return new GraphSummaryResponse(period, typeStats, dateStats, zoneStats);
    }
}
