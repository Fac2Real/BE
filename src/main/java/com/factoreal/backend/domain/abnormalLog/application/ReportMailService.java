package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse.AbnDetailResponse;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse.PeriodDetailReportResponse;
import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.factoreal.backend.domain.controlLog.application.ControlLogRepoService;
import com.factoreal.backend.domain.worker.application.WorkerService;
import com.factoreal.backend.domain.worker.dto.response.WorkerInfoResponse;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.fileUtil.CsvUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportMailService {
    private final ZoneRepoService zoneRepoService;
    private final WorkerService workerService;
    private final ReportService reportService;
    private final CsvUtil csvUtil;
    //    private final Html2PdfUtil pdfUtil;
    private final JavaMailSender mailSender;
    private final ControlLogRepoService controlLogRepoService;

//    @Scheduled(cron = "0 30 2 1 * *")   // ← 실운영용
public void sendMonthlyDetailReports() throws Exception {

        // 전체 존 목록 추출
        List<Zone> zones = zoneRepoService.findAll();

        boolean hasManager = false;

        for (Zone zone : zones) {
            String zoneId = zone.getZoneId();
            String zoneName = zone.getZoneName();

            // 공간의 매니저 탐색 후
            WorkerInfoResponse manager = workerService.getZoneManager(zoneId);

            // 담당자가 없거나 메일이 없으면 skip
            if (manager == null || manager.getEmail() == null || manager.getEmail().isBlank()) {
                log.info("[{}] 담당자 없음 → 메일 발송 생략", zoneName);
                continue;
            }

            hasManager = true;

            // for문으로 공간마다 한명인 매니저를 탐색하므로 List of를 사용함
            List<String> managerEmail = List.of(manager.getEmail());



            /* ── 1. 상세 API → CSV ───────────────────────────── */
            PeriodDetailReportResponse detail = reportService.buildLastMonthReport(/* …필요파라미터… */);

            List<Long> abnIds = detail.getZones().stream()
                    .flatMap(z -> Stream.concat(
                            z.getEnvAbnormals().stream(),
                            Stream.concat(
                                    z.getWorkers().stream().flatMap(w -> w.getWorkerAbnormals().stream()),
                                    z.getEquips().stream().flatMap(e -> e.getFacAbnormals().stream())
                            )
                    ))
                    .map(AbnDetailResponse::getAbnormalId)
                    .collect(Collectors.toList());

            Map<Long, ControlLog> ctlMap = controlLogRepoService.getControlLogs(abnIds);
            Path csv = csvUtil.writeReportAsCsv(detail, ctlMap);

            LocalDate now = LocalDate.now();
            String prevMonthStr = now.minusMonths(1)
                    .getMonth()
                    .getDisplayName(TextStyle.FULL, Locale.KOREAN);

            /* ── 3. 메일 발송 ──────────────────────────────── */
            MimeMessageHelper helper =
                    new MimeMessageHelper(mailSender.createMimeMessage(), true, "UTF-8");
            helper.setSubject("[모니토리 " + prevMonthStr + " 이상치 리포트] " + zoneName);
            helper.setTo(managerEmail.toArray(String[]::new));
            helper.setText("""
                    <p>%s - %s 담당자님, 안녕하세요. 모니토리 서비스입니다.</p>
                    <p>%s의 이상치 및 대응 현황을 첨부 드립니다.</p>
                    <ul>
                      <li>CSV : %s 이상치 데이터</li>
                    </ul>
                    """.formatted(zoneName, manager.getName(), prevMonthStr, prevMonthStr), true);
            helper.addAttachment(csv.getFileName().toString(), csv.toFile());

            mailSender.send(helper.getMimeMessage());
            log.info("[{}] 리포트 메일 발송 완료 → {}", zoneName, managerEmail);
        }

        // 모든 Zone 에서 한 번도 메일을 못 보냈다면 404
        if (!hasManager) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "모든 공간에 담당자가 존재하지 않습니다.");
        }
    }
}
