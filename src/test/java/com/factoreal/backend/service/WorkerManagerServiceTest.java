package com.factoreal.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.factoreal.backend.domain.worker.application.WorkerManagerService;
import com.factoreal.backend.domain.worker.dao.WorkerZoneRepository;
import com.factoreal.backend.domain.worker.dto.response.WorkerManagerResponse;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.worker.entity.WorkerZone;
import com.factoreal.backend.domain.worker.entity.WorkerZoneId;
import com.factoreal.backend.domain.zone.entity.Zone;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkerManagerServiceTest {

    @Mock
    private WorkerZoneRepository workerZoneRepository;

    @InjectMocks
    private WorkerManagerService workerManagerService;

    private Worker worker1, worker2, worker3, worker4;
    private Zone zone1, zone2;
    private WorkerZone workerZone1, workerZone2, workerZone3, workerZone4, workerZone5;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 테스트용 Zone 데이터 생성
        zone1 = Zone.builder()
                .zoneId("zone1")
                .zoneName("공간1")
                .build();

        zone2 = Zone.builder()
                .zoneId("zone2")
                .zoneName("공간2")
                .build();

        // 테스트용 Worker 데이터 생성
        worker1 = Worker.builder()
                .workerId("worker1")
                .name("작업자1")
                .phoneNumber("010-1111-1111")
                .email("worker1@test.com")
                .build();

        worker2 = Worker.builder()
                .workerId("worker2")
                .name("작업자2")
                .phoneNumber("010-2222-2222")
                .email("worker2@test.com")
                .build();

        worker3 = Worker.builder()
                .workerId("worker3")
                .name("작업자3")
                .phoneNumber("010-3333-3333")
                .email("worker3@test.com")
                .build();

        worker4 = Worker.builder()
                .workerId("worker4")
                .name("작업자4")
                .phoneNumber("010-4444-4444")
                .email("worker4@test.com")
                .build();

        // 테스트용 WorkerZone 데이터 생성
        workerZone1 = WorkerZone.builder()
                .id(new WorkerZoneId("worker1", "zone1"))
                .worker(worker1)
                .zone(zone1)
                .manageYn(true) // worker1은 zone1의 담당자
                .build();

        workerZone2 = WorkerZone.builder()
                .id(new WorkerZoneId("worker2", "zone1"))
                .worker(worker2)
                .zone(zone1)
                .manageYn(false) // worker2는 zone1에 접근 가능
                .build();

        workerZone3 = WorkerZone.builder()
                .id(new WorkerZoneId("worker2", "zone2"))
                .worker(worker2)
                .zone(zone2)
                .manageYn(true) // worker2는 zone2의 담당자
                .build();

        workerZone4 = WorkerZone.builder()
                .id(new WorkerZoneId("worker3", "zone1"))
                .worker(worker3)
                .zone(zone1)
                .manageYn(false) // worker3는 zone1에 접근 가능
                .build();

        workerZone5 = WorkerZone.builder()
                .id(new WorkerZoneId("worker4", "zone1"))
                .worker(worker4)
                .zone(zone1)
                .manageYn(false) // worker4는 zone1에 접근 가능
                .build();
    }

    // 특정 공간의 담당자 후보 목록 조회
    @Test
    void getManagerCandidates_WhenZoneHasManager() {
        // given
        String zoneId = "zone1";

        // 현재 담당자 설정
        when(workerZoneRepository.findByZoneZoneIdAndManageYnIsTrue(zoneId))
                .thenReturn(Optional.of(workerZone1));

        // 다른 공간의 담당자 설정
        when(workerZoneRepository.findByZoneZoneIdNotAndManageYnIsTrue(zoneId))
                .thenReturn(Arrays.asList(workerZone3)); // worker2는 zone2의 담당자

        // 해당 공간의 모든 작업자 설정
        when(workerZoneRepository.findByZoneZoneId(zoneId))
                .thenReturn(Arrays.asList(workerZone1, workerZone2, workerZone4, workerZone5));

        // when
        List<WorkerManagerResponse> candidates = workerManagerService.getManagerCandidates(zoneId);

        // then
        assertNotNull(candidates);
        assertEquals(2, candidates.size()); // worker3, worker4만 후보가 되어야 함 (현재 담당자와 다른 공간 담당자 제외)

        // worker2(다른 공간 담당자)가 후보 목록에 없는지 확인
        assertTrue(candidates.stream()
                .noneMatch(c -> c.getWorkerId().equals("worker2")));

        // worker1(현재 담당자)가 후보 목록에 없는지 확인
        assertTrue(candidates.stream()
                .noneMatch(c -> c.getWorkerId().equals("worker1")));
    }

    // 공간 담당자 지정
    @Test
    void assignManager_Success() {
        // given
        String zoneId = "zone1";
        String newManagerId = "worker3";

        // 현재 담당자 설정
        when(workerZoneRepository.findByZoneZoneIdAndManageYnIsTrue(zoneId))
                .thenReturn(Optional.of(workerZone1));

        // 새로운 담당자의 WorkerZone 설정
        WorkerZoneId newManagerZoneId = new WorkerZoneId(newManagerId, zoneId);
        when(workerZoneRepository.findById(newManagerZoneId))
                .thenReturn(Optional.of(workerZone4));

        // when
        workerManagerService.assignManager(zoneId, newManagerId);

        // then
        // 기존 담당자의 manageYn이 false로 변경되었는지 확인
        verify(workerZoneRepository, times(1))
                .save(argThat(wz -> wz.getWorker().getWorkerId().equals("worker1") && !wz.getManageYn()));

        // 새로운 담당자의 manageYn이 true로 변경되었는지 확인
        verify(workerZoneRepository, times(1))
                .save(argThat(wz -> wz.getWorker().getWorkerId().equals(newManagerId) && wz.getManageYn()));
    }

    // 현재 공간 담당자 조회 (담당자가 있는 경우)
    @Test
    void getCurrentManager_WhenManagerExists() {
        // given
        String zoneId = "zone1";
        when(workerZoneRepository.findByZoneZoneIdAndManageYnIsTrue(zoneId))
                .thenReturn(Optional.of(workerZone1));

        // when
        WorkerManagerResponse manager = workerManagerService.getCurrentManager(zoneId);

        // then
        assertNotNull(manager);
        assertEquals("worker1", manager.getWorkerId());
        assertEquals("작업자1", manager.getName());
        assertTrue(manager.getIsManager());
    }

    // 현재 공간 담당자 조회 (담당자가 없는 경우)
    @Test
    void getCurrentManager_WhenNoManagerExists() {
        // given
        String zoneId = "zone1";
        when(workerZoneRepository.findByZoneZoneIdAndManageYnIsTrue(zoneId))
                .thenReturn(Optional.empty());

        // when
        WorkerManagerResponse manager = workerManagerService.getCurrentManager(zoneId);

        // then
        assertNull(manager);
    }
}
