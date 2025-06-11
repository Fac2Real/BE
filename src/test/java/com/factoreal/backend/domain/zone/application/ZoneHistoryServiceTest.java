package com.factoreal.backend.domain.zone.application;

import com.factoreal.backend.domain.state.store.InMemoryZoneWorkerStateStore;
import com.factoreal.backend.domain.worker.application.WorkerRepoService;
import com.factoreal.backend.domain.worker.application.WorkerZoneRepoService;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.worker.entity.WorkerZone;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.global.exception.dto.BadRequestException;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneHistoryServiceTest {

    @Mock
    private InMemoryZoneWorkerStateStore zoneWorkerStateStore;

    @Mock
    private ZoneHistoryRepoService zoneHistoryRepoService;

    @Mock
    private ZoneRepoService zoneRepoService;

    @Mock
    private WorkerRepoService workerRepoService;

    @InjectMocks
    private ZoneHistoryService zoneHistoryService;

    @Captor
    private ArgumentCaptor<ZoneHist> zoneHistCaptor;

    @Mock
    private WorkerZoneRepoService workerZoneRepoService;

    private Worker worker;
    private Zone zone1, zone2, zone3;
    private WorkerZone workerZone1, workerZone2;
    private ZoneHist currentLocation;
    private LocalDateTime timestamp;

    @BeforeEach
    void setUp() {
        // 테스트용 Worker 데이터 생성
        worker = Worker.builder()
                .workerId("20250001")
                .name("홍길동")
                .email("hong@test.com")
                .phoneNumber("010-1234-5678")
                .build();

        // 테스트용 Zone 데이터 생성
        zone1 = Zone.builder()
                .zoneId("20250507165750-827")
                .zoneName("A구역")
                .build();

        zone2 = Zone.builder()
                .zoneId("20250507165751-828")
                .zoneName("B구역")
                .build();

        zone3 = Zone.builder()
                .zoneId("20250507165751-826")
                .zoneName("C구역")
                .build();

        // 테스트용 WorkerZone 데이터 생성: zone1, zone2에 대해 권한 부여
        workerZone1 = WorkerZone.builder().worker(worker).zone(zone1).manageYn(false).build();
        workerZone2 = WorkerZone.builder().worker(worker).zone(zone2).manageYn(false).build();

        // 테스트용 현재 위치 데이터 생성
        timestamp = LocalDateTime.of(2025, 5, 7, 16, 57, 50);
        currentLocation = ZoneHist.builder()
                .id(1L)
                .worker(worker)
                .zone(zone1)
                .startTime(timestamp.minusHours(1))
                .endTime(null)
                .existFlag(1)
                .build();
    }

    @Test
    @DisplayName("작업자 위치 변경 - 이전 위치가 있는 경우")
    void updateWorkerLocation_WithPreviousLocation() {
        // given
        String workerId = "20250001";
        String newZoneId = "20250507165751-828";  // zone2의 ID
        LocalDateTime newTimestamp = timestamp;
        given(workerZoneRepoService.findByWorker_WorkerId(workerId)).willReturn(List.of(workerZone1, workerZone2));

        // 작업자의 현재 위험 수준을 INFO로 설정 (정상 상태)
        given(zoneWorkerStateStore.getWorkerRiskLevel(workerId))
                .willReturn(RiskLevel.INFO);

        // 작업자의 현재 위치 정보 반환 (zone1에 있음)
        given(zoneHistoryRepoService.findByWorkerLocateForUpdate(workerId))
                .willReturn(currentLocation);

        // 작업자 정보 조회 시 미리 생성한 worker 객체 반환
        given(workerRepoService.lockWorkerRow(workerId))
                .willReturn(worker);

        // 새로운 위치(zone2) 정보 조회 시 미리 생성한 zone2 객체 반환
        given(zoneRepoService.findById(newZoneId))
                .willReturn(zone2);

        // 위치 정보 저장 시 저장하려는 객체 그대로 반환
        given(zoneHistoryRepoService.save(any(ZoneHist.class)))
                .will(invocation -> invocation.getArgument(0));

        // when
        zoneHistoryService.updateWorkerLocation(workerId, newZoneId, newTimestamp);

        // then
        // 1. 이전 위치 기록 업데이트 검증
        verify(zoneHistoryRepoService, times(2)).save(zoneHistCaptor.capture());
        List<ZoneHist> savedLocations = zoneHistCaptor.getAllValues();
        
        ZoneHist updatedOldLocation = savedLocations.get(0);
        assertThat(updatedOldLocation.getEndTime()).isEqualTo(newTimestamp);
        assertThat(updatedOldLocation.getExistFlag()).isEqualTo(0);

        // 2. 새로운 위치 기록 생성 검증
        ZoneHist newLocation = savedLocations.get(1);
        assertThat(newLocation.getWorker()).isEqualTo(worker);
        assertThat(newLocation.getZone()).isEqualTo(zone2);
        assertThat(newLocation.getStartTime()).isEqualTo(newTimestamp);
        assertThat(newLocation.getEndTime()).isNull();
        assertThat(newLocation.getExistFlag()).isEqualTo(1);

        // 3. 상태 저장소 업데이트 검증
        verify(zoneWorkerStateStore, times(1))
                .setWorkerRiskLevel(newZoneId, workerId, RiskLevel.INFO);
    }

    @Test
    @DisplayName("작업자 위치 변경 - 이전 위치가 없는 경우 (최초 입장)")
    void updateWorkerLocation_WithoutPreviousLocation() {
        // given
        String workerId = "20250001";
        String newZoneId = "20250507165750-827";  // zone1의 ID
        LocalDateTime newTimestamp = timestamp;
        given(workerZoneRepoService.findByWorker_WorkerId(workerId)).willReturn(List.of(workerZone1, workerZone2));


        // 작업자의 현재 위험 수준을 INFO로 설정 (정상 상태)
        given(zoneWorkerStateStore.getWorkerRiskLevel(workerId))
                .willReturn(RiskLevel.INFO);

        // 작업자 정보 조회 시 미리 생성한 worker 객체 반환
        given(workerRepoService.lockWorkerRow(workerId))
                .willReturn(worker);

        // 새로운 위치(zone1) 정보 조회 시 미리 생성한 zone1 객체 반환
        given(zoneRepoService.findById(newZoneId))
                .willReturn(zone1);

        // 위치 정보 저장 시 저장하려는 객체 그대로 반환
        given(zoneHistoryRepoService.save(any(ZoneHist.class)))
                .will(invocation -> invocation.getArgument(0));

        // when
        zoneHistoryService.updateWorkerLocation(workerId, newZoneId, newTimestamp);

        // then
        // 1. 새로운 위치 기록만 생성되었는지 검증
        verify(zoneHistoryRepoService, times(1)).save(zoneHistCaptor.capture());
        ZoneHist newLocation = zoneHistCaptor.getValue();

        assertThat(newLocation.getWorker()).isEqualTo(worker);
        assertThat(newLocation.getZone()).isEqualTo(zone1);
        assertThat(newLocation.getStartTime()).isEqualTo(newTimestamp);
        assertThat(newLocation.getEndTime()).isNull();
        assertThat(newLocation.getExistFlag()).isEqualTo(1);

        // 2. 상태 저장소 업데이트 검증
        verify(zoneWorkerStateStore, times(1))
                .setWorkerRiskLevel(newZoneId, workerId, RiskLevel.INFO);
    }

    @Test
    @DisplayName("작업자 위치 변경 - timestamp가 null인 경우")
    void updateWorkerLocation_WithNullTimestamp() {
        // given
        String workerId = "20250001";
        String newZoneId = "20250507165750-827";  // zone1의 ID
        given(workerZoneRepoService.findByWorker_WorkerId(workerId)).willReturn(List.of(workerZone1, workerZone2));


        // 작업자의 현재 위험 수준을 INFO로 설정 (정상 상태)
//        given(zoneWorkerStateStore.getWorkerRiskLevel(workerId))
//                .willReturn(RiskLevel.INFO);

        // 작업자의 현재 위치 정보가 없음을 설정
//        given(zoneHistoryRepoService.getCurrentWorkerLocation(workerId))
//                .willReturn(null);

        // 작업자 정보 조회 시 미리 생성한 worker 객체 반환
        given(workerRepoService.lockWorkerRow(workerId))
                .willReturn(worker);

        // 새로운 위치(zone1) 정보 조회 시 미리 생성한 zone1 객체 반환
        given(zoneRepoService.findById(newZoneId))
                .willReturn(zone1);

        // 위치 정보 저장 시 startTime이 null이면 현재 시간으로 설정하여 반환
        given(zoneHistoryRepoService.save(any(ZoneHist.class)))
                .will(invocation -> {
                    ZoneHist savedZoneHist = invocation.getArgument(0);
                    if (savedZoneHist.getStartTime() == null) {
                        savedZoneHist.setStartTime(LocalDateTime.now());
                    }
                    return savedZoneHist;
                });

        // when
        zoneHistoryService.updateWorkerLocation(workerId, newZoneId, null);

        // then
        verify(zoneHistoryRepoService, times(1)).save(zoneHistCaptor.capture());
        ZoneHist newLocation = zoneHistCaptor.getValue();

        assertThat(newLocation.getWorker()).isEqualTo(worker);
        assertThat(newLocation.getZone()).isEqualTo(zone1);
        assertThat(newLocation.getStartTime()).isNotNull(); // 현재 시간이 자동으로 설정되어야 함
        assertThat(newLocation.getEndTime()).isNull();
        assertThat(newLocation.getExistFlag()).isEqualTo(1);
    }

    @Test
    @DisplayName("작업자 위치 변경 - 위험 수준이 있는 경우")
    void updateWorkerLocation_WithRiskLevel() {
        // given
        String workerId = "20250001";
        String newZoneId = "20250507165750-827";  // zone1의 ID
        LocalDateTime newTimestamp = timestamp;
        given(workerZoneRepoService.findByWorker_WorkerId(workerId)).willReturn(List.of(workerZone1, workerZone2));


        // 작업자의 현재 위험 수준을 WARNING으로 설정 (경고 상태)
        given(zoneWorkerStateStore.getWorkerRiskLevel(workerId))
                .willReturn(RiskLevel.WARNING);

        // 작업자의 현재 위치 정보가 없음을 설정
//        given(zoneHistoryRepoService.getCurrentWorkerLocation(workerId))
//                .willReturn(null);

        // 작업자 정보 조회 시 미리 생성한 worker 객체 반환
        given(workerRepoService.lockWorkerRow(workerId))
                .willReturn(worker);

        // 새로운 위치(zone1) 정보 조회 시 미리 생성한 zone1 객체 반환
        given(zoneRepoService.findById(newZoneId))
                .willReturn(zone1);

        // 위치 정보 저장 시 저장하려는 객체 그대로 반환
        given(zoneHistoryRepoService.save(any(ZoneHist.class)))
                .will(invocation -> invocation.getArgument(0));

        // when
        zoneHistoryService.updateWorkerLocation(workerId, newZoneId, newTimestamp);

        // then
        // 1. 위치 기록 생성 검증
        verify(zoneHistoryRepoService, times(1)).save(any(ZoneHist.class));

        // 2. 위험 수준이 상태 저장소에 정확히 반영되었는지 검증
        verify(zoneWorkerStateStore, times(1))
                .setWorkerRiskLevel(newZoneId, workerId, RiskLevel.WARNING);
    }

    @Test
    @DisplayName("작업자 위치 변경 - 권한이 없는 경우 예외 반환")
    void updateWorkerLocation_WithoutAuthorization() {
        // given
        String workerId = "20250001";
        String newZoneId = "20250507165751-826";  // zone3의 ID
        LocalDateTime newTimestamp = timestamp;

        // 작업자의 현재 위험 수준을 INFO로 설정 (정상 상태)
        given(zoneWorkerStateStore.getWorkerRiskLevel(workerId))
                .willReturn(RiskLevel.INFO);

        // when & then
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> zoneHistoryService.updateWorkerLocation(workerId, newZoneId, newTimestamp)
        );
        assertThat(exception.getMessage())
                .isEqualTo("작업자 ID: 20250001는 공간 ID: 20250507165751-826에 대한 접근 권한이 없습니다.");
    }
} 