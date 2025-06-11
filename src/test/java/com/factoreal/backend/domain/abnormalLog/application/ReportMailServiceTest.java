package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse.*;
import com.factoreal.backend.domain.controlLog.application.ControlLogRepoService;
import com.factoreal.backend.domain.worker.application.WorkerManagerService;
import com.factoreal.backend.domain.worker.dto.response.WorkerManagerResponse;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.fileUtil.CsvUtil;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportMailServiceTest {


    @Mock ZoneRepoService zoneRepoService;
    @Mock WorkerManagerService workerManagerService;
    @Mock ReportService reportService;
    @Mock CsvUtil csvUtil;
    @Mock
    JavaMailSender mailSender;
    @Mock
    ControlLogRepoService controlLogRepoService;

    @InjectMocks
    ReportMailService mailService;

    /* ── 공용 더미 ───────────────────────── */
    private Zone z(String id) { return Zone.builder().zoneId(id).zoneName("Z" + id).build(); }
    private WorkerManagerResponse mgr(String email) {
        return WorkerManagerResponse.builder()
                .workerId("W1").name("홍길동").email(email).isManager(true).build();
    }

    /* 2-1. 하나라도 메일 발송되면 OK */
    // 1. 매니징 관련 정보가 잘 체크되는지, 2. 빈 csv 파일이 생성되어 잘 전달되는지 체크
    @Test
    void sendReport_atLeastOneManager_success() throws Exception {
        List <Zone> zoneList = List.of(z("A"), z("B"), z("C"));
        when(zoneRepoService.findAll()).thenReturn(zoneList);

        // A는 담당자 없는 경우
        when(workerManagerService.getCurrentManager("A")).thenReturn(null);
        // B는 담당자가 있고, 이메일도 갖는 경우
        when(workerManagerService.getCurrentManager("B")).thenReturn(mgr("test@x.com"));
        // C : 담당자 있으나 이메일은 갖고있지 않은 경우 → skip 분기
        when(workerManagerService.getCurrentManager("C")).thenReturn(mgr("  "));

        // 해당 구간에서 eq(rpt())가 실행될 때 정상적인 로직이 작동한 게 아닌
        // 첫번째 rpt()인 report 와 두번째 rpt() 가 생성되려고 시도하다가
        // 다른 주소를 바라봐서 생성하지 않고 null 이 들어가는 상황이 발생하였다.
        // 결국 전달하려는 report 가 null 과 달라 테스트 실패 발생
//        PeriodDetailReportResponse report = rpt();
//        System.out.println(rpt());
//        when(reportService.buildLastMonthReport()).thenReturn(report);
//
//        Path temp = Files.createTempFile("t", ".csv");
//        when(csvUtil.writeReportAsCsv(eq(rpt()), anyMap()))
//                .thenReturn(temp);


        /* ── 상세 리포트 mock (env 1·worker 1·equip 1) ───────── */
        AbnDetailResponse env  = AbnDetailResponse.builder().abnormalId(1L).build();
        AbnDetailResponse work = AbnDetailResponse.builder().abnormalId(2L).build();
        AbnDetailResponse fac  = AbnDetailResponse.builder().abnormalId(3L).build();

        WorkerBlockResponse wb = WorkerBlockResponse.builder()
                .workerAbnormals(List.of(work)).build();
        EquipBlockResponse  eb = EquipBlockResponse.builder()
                .facAbnormals(List.of(fac)).build();

        ZoneBlockResponse zb = ZoneBlockResponse.builder()
                .envAbnormals(List.of(env))
                .workers(List.of(wb))
                .equips(List.of(eb))
                .build();

        PeriodDetailReportResponse detail =
                new PeriodDetailReportResponse("기간", List.of(zb));

        when(reportService.buildLastMonthReport()).thenReturn(detail);
        Path temp = Files.createTempFile("t", ".csv");
        when(csvUtil.writeReportAsCsv(eq(detail), anyMap()))
                .thenReturn(temp);
        List<Long> abnIds = detail.getZones().stream()
                .flatMap(z -> Stream.concat(
                        z.getEnvAbnormals().stream(),
                        Stream.concat(
                                z.getWorkers().stream().flatMap(w -> w.getWorkerAbnormals().stream()),
                                z.getEquips().stream().flatMap(e -> e.getFacAbnormals().stream())
                        )
                ))
                .map(AbnDetailResponse::getAbnormalId)
                .toList();

        when(controlLogRepoService.getControlLogs(abnIds)).thenReturn(Map.of());
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));

        // 실행 검증
        assertDoesNotThrow(() -> mailService.sendMonthlyDetailReports());
        verify(mailSender).send(any(MimeMessage.class));
    }

    /* 2-2. 모든 Zone 에 담당자 없으면 404 에러 체크 */
    @Test
    void sendReport_noManagers_throws404() {
        when(zoneRepoService.findAll()).thenReturn(List.of(z("A"), z("B")));
        when(workerManagerService.getCurrentManager(anyString())).thenReturn(null); // 전부 null

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> mailService.sendMonthlyDetailReports());

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}