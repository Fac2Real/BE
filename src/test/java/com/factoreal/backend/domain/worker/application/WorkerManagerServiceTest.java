package com.factoreal.backend.domain.worker.application;

import com.factoreal.backend.domain.worker.dto.response.WorkerManagerResponse;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.worker.entity.WorkerZone;
import com.factoreal.backend.domain.worker.entity.WorkerZoneId;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.exception.dto.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerManagerServiceTest {

    @Mock
    private WorkerZoneRepoService workerZoneRepoService;

    @InjectMocks
    private WorkerManagerService workerManagerService;

    private Worker worker1, worker2, worker3;
    private Zone zone1, zone2;
    private WorkerZone workerZone1, workerZone2, workerZone3;

    @BeforeEach
    void setUp() {
        // 테스트용 Zone 데이터 생성
        zone1 = Zone.builder()
                .zoneId("20250507165750-827")
                .zoneName("A구역")
                .build();

        zone2 = Zone.builder()
                .zoneId("20250507165751-828")
                .zoneName("B구역")
                .build();

        // 테스트용 Worker 데이터 생성
        worker1 = Worker.builder()
                .workerId("20250001")
                .name("홍길동")
                .email("hong@test.com")
                .phoneNumber("010-1234-5678")
                .build();

        worker2 = Worker.builder()
                .workerId("20250002")
                .name("김철수")
                .email("kim@test.com")
                .phoneNumber("010-2345-6789")
                .build();

        worker3 = Worker.builder()
                .workerId("20250003")
                .name("이영희")
                .email(null)  // 이메일이 null인 경우
                .phoneNumber("010-3456-7890")
                .build();

        // 테스트용 WorkerZone 데이터 생성
        workerZone1 = WorkerZone.builder()
                .id(new WorkerZoneId(worker1.getWorkerId(), zone1.getZoneId()))
                .worker(worker1)
                .zone(zone1)
                .manageYn(true)  // worker1은 zone1의 담당자
                .build();

        workerZone2 = WorkerZone.builder()
                .id(new WorkerZoneId(worker2.getWorkerId(), zone2.getZoneId()))
                .worker(worker2)
                .zone(zone2)
                .manageYn(true)  // worker2는 zone2의 담당자
                .build();

        workerZone3 = WorkerZone.builder()
                .id(new WorkerZoneId(worker3.getWorkerId(), zone1.getZoneId()))
                .worker(worker3)
                .zone(zone1)
                .manageYn(false)  // worker3는 zone1에 접근 권한만 있음
                .build();
    }

    @Test
    @DisplayName("특정 공간의 담당자 후보 목록 조회 - 담당자가 있는 경우")
    void getManagerCandidates_WhenZoneHasManager() {
        // given
        String zoneId = "20250507165750-827";  // zone1의 ID

        given(workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId))
                .willReturn(Optional.of(workerZone1));  // 현재 담당자 설정

        given(workerZoneRepoService.findByZoneZoneIdNotAndManageYnIsTrue(zoneId))
                .willReturn(Arrays.asList(workerZone2));  // 다른 공간의 담당자 설정

        given(workerZoneRepoService.findByZoneZoneId(zoneId))
                .willReturn(Arrays.asList(workerZone1, workerZone3));  // 해당 공간의 모든 작업자 설정

        // when
        List<WorkerManagerResponse> candidates = workerManagerService.getManagerCandidates(zoneId);

        // then
        assertThat(candidates).isNotNull();
        assertThat(candidates).hasSize(1);  // worker3만 후보가 되어야 함
        
        WorkerManagerResponse candidate = candidates.get(0);
        assertThat(candidate.getWorkerId()).isEqualTo(worker3.getWorkerId());
        assertThat(candidate.getName()).isEqualTo(worker3.getName());
        assertThat(candidate.getEmail()).isNull();  // 이메일이 null인 경우 확인
        assertThat(candidate.getPhoneNumber()).isEqualTo(worker3.getPhoneNumber());
        assertThat(candidate.getIsManager()).isFalse();
    }

    @Test
    @DisplayName("특정 공간의 담당자 후보 목록 조회 - 담당자가 없는 경우")
    void getManagerCandidates_WhenZoneHasNoManager() {
        // given
        String zoneId = "20250507165750-827";  // zone1의 ID

        given(workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId))
                .willReturn(Optional.empty());  // 현재 담당자 없음

        given(workerZoneRepoService.findByZoneZoneIdNotAndManageYnIsTrue(zoneId))
                .willReturn(Arrays.asList(workerZone2));  // 다른 공간의 담당자 설정

        given(workerZoneRepoService.findByZoneZoneId(zoneId))
                .willReturn(Arrays.asList(workerZone3));  // 해당 공간의 작업자 존재

        // when
        List<WorkerManagerResponse> candidates = workerManagerService.getManagerCandidates(zoneId);

        // then
        assertThat(candidates).isNotNull();
        assertThat(candidates).hasSize(1);  // worker3만 후보가 되어야 함
        
        WorkerManagerResponse candidate = candidates.get(0);
        assertThat(candidate.getWorkerId()).isEqualTo(worker3.getWorkerId());
        assertThat(candidate.getIsManager()).isFalse();
    }

    @Test
    @DisplayName("특정 공간의 담당자 지정 - 성공")
    void assignManager_Success() {
        // given
        String zoneId = "20250507165750-827";  // zone1의 ID
        String newManagerId = "20250003";  // worker3의 ID

        given(workerZoneRepoService.findById(any(WorkerZoneId.class)))
                .willReturn(Optional.of(workerZone3));  // 새로운 담당자의 WorkerZone 설정

        given(workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId))
                .willReturn(Optional.of(workerZone1));  // 현재 담당자 설정

        // when
        workerManagerService.assignManager(zoneId, newManagerId);

        // then
        verify(workerZoneRepoService, times(1))
                .save(argThat(wz -> wz.getWorker().getWorkerId().equals(worker1.getWorkerId()) 
                        && !wz.getManageYn()));  // 기존 담당자의 manageYn이 false로 변경

        verify(workerZoneRepoService, times(1))
                .save(argThat(wz -> wz.getWorker().getWorkerId().equals(newManagerId) 
                        && wz.getManageYn()));  // 새로운 담당자의 manageYn이 true로 변경
    }

    @Test
    @DisplayName("특정 공간의 담당자 지정 - 실패 (접근 권한 없음)")
    void assignManager_Fail_NoAccess() {
        // given
        String zoneId = "20250507165750-827";  // zone1의 ID
        String newManagerId = "20250002";  // worker2의 ID (zone1에 접근 권한 없음)

        given(workerZoneRepoService.findById(any(WorkerZoneId.class)))
                .willReturn(Optional.empty());  // 접근 권한 없음

        // when & then
        assertThatThrownBy(() -> workerManagerService.assignManager(zoneId, newManagerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("접근 권한이 없습니다");
    }

    @Test
    @DisplayName("현재 공간 담당자 조회 - 담당자가 있는 경우")
    void getCurrentManager_WhenManagerExists() {
        // given
        String zoneId = "20250507165750-827";  // zone1의 ID

        given(workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId))
                .willReturn(Optional.of(workerZone1));

        // when
        WorkerManagerResponse manager = workerManagerService.getCurrentManager(zoneId);

        // then
        assertThat(manager).isNotNull();
        assertThat(manager.getWorkerId()).isEqualTo(worker1.getWorkerId());
        assertThat(manager.getName()).isEqualTo(worker1.getName());
        assertThat(manager.getEmail()).isEqualTo(worker1.getEmail());
        assertThat(manager.getPhoneNumber()).isEqualTo(worker1.getPhoneNumber());
        assertThat(manager.getIsManager()).isTrue();
    }

    @Test
    @DisplayName("현재 공간 담당자 조회 - 담당자가 없는 경우")
    void getCurrentManager_WhenNoManagerExists() {
        // given
        String zoneId = "20250507165750-827";  // zone1의 ID

        given(workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId))
                .willReturn(Optional.empty());

        // when
        WorkerManagerResponse manager = workerManagerService.getCurrentManager(zoneId);

        // then
        assertThat(manager).isNull();
    }
} 