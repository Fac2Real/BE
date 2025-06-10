package com.factoreal.backend.domain.controlLog.application;

import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.controlLog.dao.ControlLogRepository;
import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.messaging.mqtt.MqttPublishService;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControlLogServiceTest {

    /* ── MOCKS ───────────────────────────────────────── */
    @Mock ControlLogRepository repo;
    @Mock MqttPublishService   mqtt;
    @Mock WebSocketSender      socket;

    @InjectMocks
    ControlLogService service;

    /* 공용 더미 */
    private AbnormalLog abn(long id) {
        return AbnormalLog.builder().id(id).build();
    }
    private Zone zone() { return Zone.builder().zoneId("Z1").zoneName("대기실").build(); }

    /* ────────────────────────────────────────────────── */

    @Test
    @DisplayName("saveControlLog() : 제어-로그 저장 + 발행 성공")
    void saveControl_success() throws JsonProcessingException {
        // given
        AbnormalLog src = abn(10L);
        Zone        z   = zone();

        // 저장되는 ControlLog 를 그대로 반환하게 스텁
        when(repo.save(any(ControlLog.class)))
                .thenAnswer(inv -> {
                    ControlLog cl = inv.getArgument(0, ControlLog.class);
                    // id 할당(가짜 PK) 99L의 Id로
                    cl.setId(99L);
                    return cl;
                });

        // when
        ControlLog saved = service.saveControlLog(src, "FAN", 80D, 1, z);

        // then
        assertThat(saved.getId()).isEqualTo(99L);
        assertThat(saved.getControlType()).isEqualTo("FAN");
        assertThat(saved.getControlVal()).isEqualTo(80D);
        assertThat(saved.getControlStat()).isEqualTo(1);
        assertThat(saved.getAbnormalLog()).isSameAs(src);
        assertThat(saved.getZone()).isSameAs(z);
        assertThat(saved.getExecutedAt())
                .isBeforeOrEqualTo(LocalDateTime.now());   // 실행 시각

        // MQ & WebSocket 1회씩 호출
        verify(mqtt).publishControlMessage(saved);
        verify(socket).sendControlStatus(eq(saved), anyMap());
        /* 👁️ 2) Pretty JSON */
        ObjectMapper om = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println("=== saved ControlLog (JSON) ===");
        System.out.println(om.writeValueAsString(saved));
    }

    /* ── getControlLogs()  ───────────────────────────── */

    @Test
    @DisplayName("getControlLogs() : ID-목록으로 Map 조회")
    void getControlLogs_success() throws JsonProcessingException {
        // given : 2개의 제어 로그
        AbnormalLog a1 = abn(1L);
        AbnormalLog a2 = abn(2L);

        ControlLog  c1 = ControlLog.builder().id(101L).abnormalLog(a1).build();
        ControlLog  c2 = ControlLog.builder().id(102L).abnormalLog(a2).build();

        when(repo.findByAbnormalLog_IdIn(List.of(1L, 2L)))
                .thenReturn(List.of(c1, c2));

        // when
        Map<Long, ControlLog> map = new ControlLogRepoService(repo)
                .getControlLogs(List.of(1L, 2L));

        // then
        assertThat(map).hasSize(2)
                .containsEntry(1L, c1)
                .containsEntry(2L, c2);

        /* 👁️ Map key → JSON 출력 */
        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println("=== ControlLog Map ===");
        System.out.println(om.writeValueAsString(map));

        // 개별 객체 확인도 가능
        map.values().forEach(System.out::println);
    }
}