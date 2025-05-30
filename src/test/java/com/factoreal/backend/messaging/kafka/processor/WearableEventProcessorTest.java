package com.factoreal.backend.messaging.kafka.processor;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.stateStore.InMemoryZoneWorkerStateStore;
import com.factoreal.backend.domain.worker.application.WorkerRepoService;
import com.factoreal.backend.domain.worker.application.WorkerZoneRepoService;
import com.factoreal.backend.domain.worker.entity.WorkerZone;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.messaging.kafka.dto.WearableKafkaDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.messaging.service.AlarmEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 단위 테스트: WearableEventProcessor
 *
 * <p>Kafka로부터 수신된 생체 데이터 위험도가 변경될 때
 * 1) AbnormalLog 저장
 * 2) WebSocket 알림 전송
 * 3) In-memory 상태 스토어 갱신
 * 이 세 가지가 정상 동작하는지 검증한다.
 */
class WearableEventProcessorTest {

    // ── mocks ────────────────────────────────────────────────
    private final AbnormalLogService abnormalSvc = mock(AbnormalLogService.class);
    private final AbnormalLogRepoService repoSvc = mock(AbnormalLogRepoService.class);
    private final WebSocketSender ws = mock(WebSocketSender.class);
    private final AlarmEventService alarmSvc = mock(AlarmEventService.class);
    private final WorkerZoneRepoService workerZoneRepo = mock(WorkerZoneRepoService.class);
    private final ZoneRepoService zoneRepo = mock(ZoneRepoService.class);
    private final WorkerRepoService workerRepo = mock(WorkerRepoService.class);

    // ── system under test ────────────────────────────────────
    private final InMemoryZoneWorkerStateStore store = new InMemoryZoneWorkerStateStore();
    private WearableEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new WearableEventProcessor(
                abnormalSvc,
                repoSvc,
                ws,
                alarmSvc,
                workerZoneRepo,
                zoneRepo,
                workerRepo,
                store          // 마지막 인자로 상태 스토어
        );
    }

    @Test
    @DisplayName("위험도가 변하면 abnormalLog 저장·위험 알림·상태 업데이트가 수행된다")
    void process_shouldHandleWearableEvent_whenRiskLevelChanged() {
        // ── arrange ───────────────────────────────────────────
        String zoneId = "Z1";
        String workerId = "W1";

        // 기존 위험도 = INFO
        store.setWorkerRiskLevel(zoneId, workerId, RiskLevel.INFO);
        assertThat(store.getWorkerRiskLevel(zoneId, workerId)).isEqualTo(RiskLevel.INFO);
        assertThat(store.getZoneRiskLevel(zoneId)).isEqualTo(RiskLevel.INFO);

        WearableKafkaDto dto = new WearableKafkaDto();
        dto.setWorkerId(workerId);
        dto.setSensorType(WearableDataType.heartRate.name());
        dto.setDangerLevel(2);               // CRITICAL
        dto.setWearableDeviceId("DEV1");

        // WorkerZone ↔ Zone 매핑 stub
        Zone zone = mock(Zone.class);
        when(zone.getZoneId()).thenReturn(zoneId);
        WorkerZone workerZone = mock(WorkerZone.class);
        when(workerZone.getZone()).thenReturn(zone);
        when(workerZoneRepo.findByWorker_WorkerId(workerId))
                .thenReturn(List.of(workerZone));

        // abnormalLog stub
        AbnormalLog abnormalLog = mock(AbnormalLog.class);
        when(abnormalLog.getZone()).thenReturn(zone);
        when(abnormalSvc.saveAbnormalLogFromWearableKafkaDto(
                any(WearableKafkaDto.class),
                any(WearableDataType.class),
                any(RiskLevel.class),
                any(TargetType.class)))
                .thenReturn(abnormalLog);

        // 기타 stub
        when(repoSvc.countByIsReadFalse()).thenReturn(5L);
        AlarmEventDto alarmDto = mock(AlarmEventDto.class);
        when(alarmSvc.generateAlarmDto(any(), any(), any())).thenReturn(alarmDto);

        // ── act ───────────────────────────────────────────────
        processor.process(dto, "WEARABLE");

        // ── assert ────────────────────────────────────────────
        // 1) abnormalLog 저장
        verify(abnormalSvc).saveAbnormalLogFromWearableKafkaDto(
                eq(dto),
                eq(WearableDataType.heartRate),
                eq(RiskLevel.CRITICAL),
                eq(TargetType.Worker)
        );

        // 2) WebSocket 알림 전송
        verify(ws).sendDangerLevel(zoneId, WearableDataType.heartRate.name(), 2);
        verify(ws).sendDangerAlarm(alarmDto);
        verify(ws).sendUnreadCount(5L);

        // 3) in-memory 상태 업데이트
        assertThat(store.getWorkerRiskLevel(zoneId, workerId))
                .isEqualTo(RiskLevel.CRITICAL);
        assertThat(store.getZoneRiskLevel(zoneId))
                .isEqualTo(RiskLevel.CRITICAL);
    }
}
