package com.factoreal.backend.messaging.kafka.processor;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.stateStore.InMemoryZoneWorkerStateStore;
import com.factoreal.backend.domain.zone.application.ZoneHistoryRepoService;
import com.factoreal.backend.domain.zone.application.ZoneHistoryService;
import com.factoreal.backend.messaging.kafka.dto.WearableKafkaDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import com.factoreal.backend.messaging.sender.WebSocketSender;
import com.factoreal.backend.messaging.service.AlarmEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 단위 테스트: WearableEventProcessor
 * <p>
 * Kafka로부터 수신된 생체 데이터 위험도가 변경될 때
 * 1) AbnormalLog 저장
 * 2) WebSocket 알림 전송
 * 3) In-memory 상태 스토어 갱신
 * 4) zoneId가 없을 때 기본 존으로 업데이트
 */
class WearableEventProcessorTest {

    // ── mocks ────────────────────────────────────────────────
    private AbnormalLogService abnormalSvc = mock(AbnormalLogService.class);
    private AbnormalLogRepoService repoSvc = mock(AbnormalLogRepoService.class);
    private WebSocketSender ws = mock(WebSocketSender.class);
    private AlarmEventService alarmSvc = mock(AlarmEventService.class);
    private ZoneHistoryService historySvc = mock(ZoneHistoryService.class);
    private ZoneHistoryRepoService historyRepo = mock(ZoneHistoryRepoService.class);

    // ── system under test ────────────────────────────────────
    private InMemoryZoneWorkerStateStore store = new InMemoryZoneWorkerStateStore();
    private WearableEventProcessor processor;

    // 공통 상수
    static final String WORKER_ID = "W1";
    static final String ZONE_ID = "Z1";
    static final String DEFAULT_ZONE = "00000000000000-000";

    /**
     * 테스트용 DTO 생성 헬퍼
     */
    private WearableKafkaDto makeDto() {
        WearableKafkaDto dto = new WearableKafkaDto();
        dto.setWorkerId(WORKER_ID);
        dto.setSensorType(WearableDataType.heartRate.name()); // enum 이름 그대로
        dto.setDangerLevel(2);    // CRITICAL
        dto.setWearableDeviceId("DEV1");
        return dto;
    }

    @BeforeEach
    void setUp() {
        processor = new WearableEventProcessor(
                abnormalSvc,
                repoSvc,
                ws,
                alarmSvc,
                store,
                historySvc,
                historyRepo
        );
        // 공통 stub
        when(repoSvc.countByIsReadFalse()).thenReturn(3L);
        when(alarmSvc.generateAlarmDto(any(), any(), any()))
                .thenReturn(mock(AlarmEventDto.class));
        when(abnormalSvc.saveAbnormalLogFromWearableKafkaDto(
                any(), any(), any(), any()))
                .thenReturn(mock(AbnormalLog.class));
    }

    /**
     * WearableDataType.getWearableType(...)를 스태틱 목킹
     */
    private MockedStatic<WearableDataType> mockWearableType() {
        MockedStatic<WearableDataType> wearMock = mockStatic(WearableDataType.class);
        wearMock.when(() -> WearableDataType.getWearableType(anyString()))
                .thenReturn(WearableDataType.heartRate);
        return wearMock;
    }

    @Nested
    class WhenZoneIdAlreadyInStore {

        @Test
        @DisplayName("A) 스토어에 zoneId가 있으면 updateWorkerLocation을 호출하지 않는다")
        void process_WithExistingZoneId() {
            try (MockedStatic<WearableDataType> ignored = mockWearableType()) {
                // ── arrange ─────────────────────────────
                store.setWorkerRiskLevel(ZONE_ID, WORKER_ID, RiskLevel.INFO);

                // ── act ────────────────────────────────
                processor.process(makeDto(), "WEARABLE");

                // ── assert ────────────────────────────
                // 이미 스토어에 zoneId가 있으므로 updateWorkerLocation 호출 안 함
                verify(historySvc, never()).updateWorkerLocation(any(), any(), any());

                // 히트맵, 알림 저장, 상태 업데이트가 정상적으로 수행됨
                verify(ws).sendDangerLevel(ZONE_ID, "heartRate", 2);
                verify(abnormalSvc).saveAbnormalLogFromWearableKafkaDto(
                        any(), eq(WearableDataType.heartRate),
                        eq(RiskLevel.CRITICAL), eq(TargetType.Worker)
                );
                // 상태 저장소에 CRITICAL로 업데이트
                assertThat(store.getWorkerRiskLevel(WORKER_ID)).isEqualTo(RiskLevel.CRITICAL);
                assertThat(store.getZoneRiskLevel(ZONE_ID)).isEqualTo(RiskLevel.CRITICAL);
            }
        }
    }

    @Nested
    class WhenZoneIdMissing {

        @Test
        @DisplayName("B) 스토어와 히스토리에 zoneId가 없으면 DEFAULT_ZONE_ID로 updateWorkerLocation 호출")
        void process_WithDefaultZoneFallback() {
            try (MockedStatic<WearableDataType> ignored = mockWearableType()) {
                // ── arrange ─────────────────────────────
                // 1) 스토어에 없음
                // 2) historyRepo에도 매핑이 없으므로 null 반환
                when(historyRepo.getCurrentWorkerLocation(WORKER_ID)).thenReturn(null);

                // updateWorkerLocation(...)은 void 메서드이므로 doAnswer로 처리
                doAnswer(invocation -> {
                    // "기본 존"으로 매핑하여 상태 저장소에 INFO로 초기화
                    store.setWorkerRiskLevel(DEFAULT_ZONE, WORKER_ID, RiskLevel.INFO);
                    return null;
                }).when(historySvc).updateWorkerLocation(
                        eq(WORKER_ID),
                        eq(DEFAULT_ZONE),
                        any(LocalDateTime.class)
                );

                // ── act ────────────────────────────────
                processor.process(makeDto(), "WEARABLE");

                // ── assert ────────────────────────────
                // updateWorkerLocation이 호출되어야 함
                verify(historySvc).updateWorkerLocation(
                        eq(WORKER_ID), eq(DEFAULT_ZONE), any(LocalDateTime.class)
                );

                // 기본 존 ID로 히트맵 전송
                verify(ws).sendDangerLevel(DEFAULT_ZONE, "heartRate", 2);

                // 상태 저장소에 기본 존으로 매핑되어 있음
                assertThat(store.getZoneId(WORKER_ID)).isEqualTo(DEFAULT_ZONE);
                // 이후 리스크가 CRITICAL로 업데이트됨
                assertThat(store.getWorkerRiskLevel(WORKER_ID)).isEqualTo(RiskLevel.CRITICAL);
                assertThat(store.getZoneRiskLevel(DEFAULT_ZONE)).isEqualTo(RiskLevel.CRITICAL);
            }
        }
    }
}
