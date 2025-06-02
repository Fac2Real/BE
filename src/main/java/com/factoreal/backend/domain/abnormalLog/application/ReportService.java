package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.response.DangerStatResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.GradeSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.MonthlyDetailResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.MonthlyGradeSummaryResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse.*;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.factoreal.backend.domain.controlLog.service.ControlLogRepoService;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
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
import java.util.Objects;
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

    public PeriodDetailReport buildLast30DaysReport() {
        // ────────────────────────────────────────────────
        // 2-1. 데이터 준비
        // ────────────────────────────────────────────────

        // (a) 조회 기간: 오늘 기준 30일 전 ~ 지금
        LocalDateTime end = LocalDateTime.now().minusDays(1);
        LocalDateTime start = end.minusDays(30);

        // (b) Zone 트리 뼈대: ZoneService#getZoneItems() 호출
        //     (예: ZoneDetailResponse 에는 zoneId, zoneName, envList, equipList 등이 들어 있음)
        List<ZoneDetailResponse> zoneMeta = zoneService.getZoneItems();

        // (c) 30일치 위험·경고 이상치 AbnormalLog 리스트
        //     AbnormalLogRepoService#findPreview30daysLog() 는
        //     이미 dangerLevel==1 or 2 로 필터링된 로그를 반환한다고 가정
        List<AbnormalLog> logs = abnormalLogRepoService.findPreview30daysLog();

        // (d) AbnormalLog 의 ID 들만 추출 → ControlLog 한꺼번에 조회
        List<Long> abnIds = logs.stream()
                .map(AbnormalLog::getId)
                .toList();

        // ControlLogRepository 에는 AbnormalLog ID 리스트로 대응 기록을 모두 꺼내오는 메서드.
        Map<Long, ControlLog> ctlMap = controlLogRepoService.getControlLogs(abnIds);

        // ❶ 로그·ControlLog 준비한 후 …
        Map<String,String> sensorToEquip = sensorRepoService
                .sensorIdToEquipId(
                        logs.stream()
                                .filter(l -> l.getTargetType()==TargetType.Equip)
                                .map(AbnormalLog::getTargetId)
                                .distinct()
                                .toList()
                );

        // ────────────────────────────────────────────────
        // 2-2. Zone 트리(뼈대) + AbnormalLog + ControlLog 합치기 ++ 설비별 센서 매핑 정보도 추가
        // ────────────────────────────────────────────────

        List<ZoneBlock> zones = zoneMeta.stream()
                .map(zm -> buildZoneBlock(zm, logs, ctlMap, sensorToEquip))
                .toList();


        // ────────────────────────────────────────────────
        // 2-3. 최종 PeriodDetailReport 생성
        // ────────────────────────────────────────────────

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        String period = f.format(start.toLocalDate()) + " ~ " + f.format(end.toLocalDate());

        return new PeriodDetailReport(period, zones);
    }

    private ZoneBlock buildZoneBlock(ZoneDetailResponse zm,
                                     List<AbnormalLog> allLogs,
                                     Map<Long,ControlLog> ctlMap,
                                     Map<String,String> sensorToEquip) {

        String zid = zm.getZoneId();
        List<AbnormalLog> zLogs = allLogs.stream()
                .filter(l -> l.getZone().getZoneId().equals(zid))
                .toList();

        // ① 환경
        List<AbnDetail> envAbn = zLogs.stream()
                .filter(l -> l.getTargetType()==TargetType.Sensor)
                .map(l -> toDetail(l, ctlMap.get(l.getId())))
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
                            .toList();
                    return EquipBlock.builder()
                            .equipId(em.getEquipId())
                            .equipName(em.getEquipName())
                            .facCnt(fac.size())
                            .facAbnormals(fac)
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
                .workerAbnormals(workerAbn)
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

}
