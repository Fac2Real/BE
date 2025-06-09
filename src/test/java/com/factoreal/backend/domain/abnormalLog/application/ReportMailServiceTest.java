package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse.PeriodDetailReportResponse;
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
    private WorkerManagerResponse mgr() {
        return WorkerManagerResponse.builder()
                .workerId("W1").name("홍길동").email("test@x.com").isManager(true).build();
    }
    private PeriodDetailReportResponse rpt() {
        return new PeriodDetailReportResponse("P", List.of());
    }

    /* 2-1. 하나라도 메일 발송되면 OK */
    // 1. 매니징 관련 정보가 잘 체크되는지, 2. 빈 csv 파일이 생성되어 잘 전달되는지 체크
    @Test
    void sendReport_atLeastOneManager_success() throws Exception {
        when(zoneRepoService.findAll()).thenReturn(List.of(z("A"), z("B")));

        when(workerManagerService.getCurrentManager("A")).thenReturn(null);  // A는 담당자 없음
        when(workerManagerService.getCurrentManager("B")).thenReturn(mgr()); // B는 있음

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

        PeriodDetailReportResponse report = rpt(); //@1234
        when(reportService.buildLastMonthReport()).thenReturn(report);

        Path temp = Files.createTempFile("t", ".csv");
        when(csvUtil.writeReportAsCsv(eq(report), anyMap()))
                .thenReturn(temp);

        when(controlLogRepoService.getControlLogs(anyList())).thenReturn(Map.of());
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