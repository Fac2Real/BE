package com.factoreal.backend.domain.worker.application;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;
import com.factoreal.backend.domain.worker.dto.request.CreateWorkerRequest;
import com.factoreal.backend.domain.worker.dto.response.WorkerCurrentLocationResponse;
import com.factoreal.backend.domain.worker.dto.response.WorkerDetailResponse;
import com.factoreal.backend.domain.worker.dto.response.WorkerInfoResponse;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.worker.entity.WorkerZone;
import com.factoreal.backend.domain.worker.entity.WorkerZoneId;
import com.factoreal.backend.domain.zone.application.ZoneHistoryRepoService;
import com.factoreal.backend.domain.zone.application.ZoneHistoryService;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.global.exception.dto.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @InjectMocks
    private WorkerService workerService;

    @Mock
    private WorkerZoneRepoService workerZoneRepoService;

    @Mock
    private WorkerRepoService workerRepoService;

    @Mock
    private ZoneRepoService zoneRepoService;

    @Mock
    private AbnormalLogService abnormalLogService;

    @Mock
    private ZoneHistoryRepoService zoneHistoryRepoService;

    @Mock
    private ZoneHistoryService zoneHistoryService;

    private Worker worker;
    private Zone zone;
    private WorkerZone workerZone;
    private ZoneHist zoneHist;
    private String workerId;
    private String workerName;
    private String phoneNumber;
    private String email;
    private String zoneId;
    private String zoneName;

    @BeforeEach
    void setUp() {
        workerId = "20250001";
        workerName = "이상해씨";
        phoneNumber = "010-1000-0008";
        email = "han8@example.com";
        zoneId = "20250507165750-827";
        zoneName = "생산 라인 A";

        zone = Zone.builder()
                .zoneId(zoneId)
                .zoneName(zoneName)
                .build();

        worker = Worker.builder()
                .workerId(workerId)
                .name(workerName)
                .phoneNumber(phoneNumber)
                .email(email)
                .build();

        workerZone = WorkerZone.builder()
                .id(new WorkerZoneId(workerId, zoneId))
                .worker(worker)
                .zone(zone)
                .manageYn(false)
                .build();

        zoneHist = ZoneHist.builder()
                .worker(worker)
                .zone(zone)
                .existFlag(1)
                .startTime(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getAllWorkers 메서드 테스트")
    class GetAllWorkersTest {

        @Test
        @DisplayName("정상적인 작업자 목록 조회")
        void getAllWorkers_Success() {
            // given
            List<Worker> workers = Arrays.asList(worker);
            AbnormalLogResponse abnormalLog = AbnormalLogResponse.builder()
                .id(1L)
                .targetType(TargetType.Worker)
                .targetId(workerId)
                .abnormalType("심박수 위험")
                .abnVal(120.0)
                .dangerLevel(1)
                .detectedAt(LocalDateTime.now())
                .zoneId(zoneId) // 이상 상황이 발생한 공간 ID
                .zoneName(zoneName) // 이상 상황이 발생한 공간 이름
                .build();

            given(workerRepoService.findAll()).willReturn(workers);
            given(abnormalLogService.findLatestAbnormalLogsForTargets(any(TargetType.class), any()))
                    .willReturn(Arrays.asList(abnormalLog));
            given(zoneHistoryRepoService.findByWorker_WorkerIdAndExistFlag(anyString(), anyInt()))
                    .willReturn(zoneHist);
            given(workerZoneRepoService.findByWorker_WorkerId(workerId))
                    .willReturn(Arrays.asList(workerZone));
            given(workerZoneRepoService.findByWorkerWorkerIdAndManageYnIsTrue(workerId))
                    .willReturn(Optional.empty());

            // when
            List<WorkerDetailResponse> responses = workerService.getAllWorkers();

            // then
            assertThat(responses).hasSize(1);
            WorkerDetailResponse response = responses.get(0);
            assertThat(response.getWorkerId()).isEqualTo(workerId);
            assertThat(response.getName()).isEqualTo(workerName);
            assertThat(response.getPhoneNumber()).isEqualTo(phoneNumber);
            assertThat(response.getEmail()).isEqualTo(email);
            assertThat(response.getCurrentZoneId()).isEqualTo(zoneId);
            assertThat(response.getCurrentZoneName()).isEqualTo(zoneName);
            assertThat(response.getAccessZones()).hasSize(1);
            assertThat(response.getManagedZones()).isEmpty();
        }

        @Test
        @DisplayName("작업자가 없는 경우")
        void getAllWorkers_WhenNoWorkers() {
            // given
            given(workerRepoService.findAll()).willReturn(List.of());

            // when
            List<WorkerDetailResponse> responses = workerService.getAllWorkers();

            // then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("작업자의 현재 위치가 null인 경우")
        void getAllWorkers_WhenCurrentLocationNull() {
            // given
            List<Worker> workers = Arrays.asList(worker);
            
            given(workerRepoService.findAll()).willReturn(workers);
            given(abnormalLogService.findLatestAbnormalLogsForTargets(any(TargetType.class), any()))
                    .willReturn(List.of());
            given(zoneHistoryRepoService.findByWorker_WorkerIdAndExistFlag(anyString(), anyInt()))
                    .willReturn(null); // 현재 해당 공간에 있는 작업자가 존재하지 않음
            given(workerZoneRepoService.findByWorker_WorkerId(workerId))
                    .willReturn(List.of());
            given(workerZoneRepoService.findByWorkerWorkerIdAndManageYnIsTrue(workerId))
                    .willReturn(Optional.empty());

            // when
            List<WorkerDetailResponse> responses = workerService.getAllWorkers();

            // then
            assertThat(responses).hasSize(1);
            WorkerDetailResponse response = responses.get(0);
            assertThat(response.getCurrentZoneId()).isEqualTo("00000000000000-000");
            assertThat(response.getCurrentZoneName()).isEqualTo("대기실");
        }

        @Test
        @DisplayName("작업자의 이메일이 null인 경우")
        void getAllWorkers_WhenEmailNull() {
            // given
            Worker workerWithNullEmail = Worker.builder()
                    .workerId(workerId)
                    .name(workerName)
                    .phoneNumber(phoneNumber)
                    .email(null)
                    .build();

            given(workerRepoService.findAll()).willReturn(Arrays.asList(workerWithNullEmail));
            given(abnormalLogService.findLatestAbnormalLogsForTargets(any(TargetType.class), any()))
                    .willReturn(List.of());
            given(zoneHistoryRepoService.findByWorker_WorkerIdAndExistFlag(anyString(), anyInt()))
                    .willReturn(zoneHist);
            given(workerZoneRepoService.findByWorker_WorkerId(workerId))
                    .willReturn(List.of());
            given(workerZoneRepoService.findByWorkerWorkerIdAndManageYnIsTrue(workerId))
                    .willReturn(Optional.empty());

            // when
            List<WorkerDetailResponse> responses = workerService.getAllWorkers();

            // then
            assertThat(responses).hasSize(1);
            WorkerDetailResponse response = responses.get(0);
            assertThat(response.getEmail()).isNull();
        }
        @Test
        @DisplayName("zoneHistory 조회 실패 시 기본값 반환")
        void getAllWorkers_WhenZoneHistoryThrows() {
            // given
            List<Worker> workers = List.of(worker);
            given(workerRepoService.findAll()).willReturn(workers);
            given(zoneHistoryRepoService.findByWorker_WorkerIdAndExistFlag(anyString(), anyInt()))
                    .willThrow(new RuntimeException("DB down"));              // 예외 유도
            given(workerZoneRepoService.findByWorker_WorkerId(workerId))
                    .willReturn(List.of(workerZone));
            given(workerZoneRepoService.findByWorkerWorkerIdAndManageYnIsTrue(workerId))
                    .willReturn(Optional.empty());

            // when
            List<WorkerDetailResponse> result = workerService.getAllWorkers();

            // then
            WorkerDetailResponse dto = result.get(0);
            assertThat(dto.getCurrentZoneId()).isEqualTo("00000000000000-000");
            assertThat(dto.getCurrentZoneName()).isEqualTo("대기실");
        }
        @Test
        @DisplayName("담당 구역이 있을 때 managedZones 채워짐")
        void getAllWorkers_WhenManagerExists() {
            // given
            WorkerZone managerZone = WorkerZone.builder()
                    .id(new WorkerZoneId(workerId, zoneId))
                    .worker(worker)
                    .zone(zone)
                    .manageYn(true)
                    .build();

            given(workerRepoService.findAll()).willReturn(List.of(worker));
            given(zoneHistoryRepoService.findByWorker_WorkerIdAndExistFlag(anyString(), anyInt()))
                    .willReturn(zoneHist);
            given(workerZoneRepoService.findByWorker_WorkerId(workerId))
                    .willReturn(List.of(managerZone));                // access zones
            given(workerZoneRepoService.findByWorkerWorkerIdAndManageYnIsTrue(workerId))
                    .willReturn(Optional.of(managerZone));            // managed zone

            // when
            WorkerDetailResponse dto = workerService.getAllWorkers().get(0);

            // then
            assertThat(dto.getManagedZones()).hasSize(1);
            assertThat(dto.getManagedZones().get(0).getZoneId()).isEqualTo(zoneId);
        }
        @Test
        @DisplayName("DTO 변환 중 예외 시 기본값 반환")
        void getAllWorkers_WhenConversionFails() {
            // given: workerZoneRepoService 가 NullPointerException 유발
            given(workerRepoService.findAll()).willReturn(List.of(worker));
            given(workerZoneRepoService.findByWorker_WorkerId(workerId))
                    .willThrow(new NullPointerException("mock npe"));

            // when
            WorkerDetailResponse dto = workerService.getAllWorkers().get(0);

            // then
            assertThat(dto.getAccessZones()).isEmpty();
            assertThat(dto.getManagedZones()).isEmpty();
            assertThat(dto.getCurrentZoneId()).isEqualTo("00000000000000-000");
        }
    }

    @Nested
    @DisplayName("getWorkersByZoneId 메서드 테스트")
    class GetWorkersByZoneIdTest {

        @Test
        @DisplayName("특정 공간의 작업자 목록 조회")
        void getWorkersByZoneId_Success() {
            // given
            List<ZoneHist> zoneHists = Arrays.asList(zoneHist);
            given(zoneHistoryRepoService.findByZone_ZoneIdAndExistFlag(zoneId, 1))
                    .willReturn(zoneHists);

            // when
            List<WorkerInfoResponse> responses = workerService.getWorkersByZoneId(zoneId);

            // then
            assertThat(responses).hasSize(1);
            WorkerInfoResponse response = responses.get(0);
            assertThat(response.getWorkerId()).isEqualTo(workerId);
            assertThat(response.getName()).isEqualTo(workerName);
        }

        @Test
        @DisplayName("해당 공간에 작업자가 없는 경우")
        void getWorkersByZoneId_WhenNoWorkers() {
            // given
            given(zoneHistoryRepoService.findByZone_ZoneIdAndExistFlag(zoneId, 1))
                    .willReturn(List.of());

            // when
            List<WorkerInfoResponse> responses = workerService.getWorkersByZoneId(zoneId);

            // then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 공간 ID로 조회하는 경우")
        void getWorkersByZoneId_WithNonExistentZoneId() {
            // given
            String nonExistentZoneId = "non_existent_zone";
            given(zoneHistoryRepoService.findByZone_ZoneIdAndExistFlag(nonExistentZoneId, 1))
                    .willReturn(List.of());

            // when
            List<WorkerInfoResponse> responses = workerService.getWorkersByZoneId(nonExistentZoneId);

            // then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("createWorker 메서드 테스트")
    class CreateWorkerTest {

        private CreateWorkerRequest request;
        private Zone waitRoom;

        @BeforeEach
        void setUp() {
            request = new CreateWorkerRequest();
            request.setWorkerId(workerId);
            request.setName(workerName);
            request.setPhoneNumber(phoneNumber);
            request.setEmail(email);
            request.setZoneNames(Arrays.asList(zoneName));

            waitRoom = Zone.builder()                      // 대기실 Zone 목 객체
                    .zoneId("00000000000000-000")
                    .zoneName("대기실")
                    .build();
        }

        @Test
        @DisplayName("정상적인 작업자 생성")
        void createWorker_Success() {
            // given
            given(workerRepoService.existsByWorkerId(workerId)).willReturn(false);
            given(workerRepoService.existsByPhoneNumber(phoneNumber)).willReturn(false);
            given(zoneRepoService.findByZoneName(zoneName)).willReturn(zone);          // 생산 라인 A
            given(zoneRepoService.findByZoneName("대기실")).willReturn(waitRoom);      // 대기실

            // when
            workerService.createWorker(request);

            // then
            verify(workerRepoService, times(1)).save(any(Worker.class));
            verify(workerZoneRepoService, times(2)).save(any(WorkerZone.class));       // ✔ 1 → 2
            verify(zoneHistoryService, times(1))
                    .updateWorkerLocation(eq(workerId), eq("00000000000000-000"), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("중복된 작업자 ID인 경우")
        void createWorker_DuplicateWorkerId() {
            // given
            given(workerRepoService.existsByWorkerId(workerId)).willReturn(true);

            // when & then
            assertThrows(DuplicateResourceException.class,
                    () -> workerService.createWorker(request));
            verify(workerRepoService, never()).save(any(Worker.class));
        }

        @Test
        @DisplayName("중복된 전화번호인 경우")
        void createWorker_DuplicatePhoneNumber() {
            // given
            given(workerRepoService.existsByWorkerId(workerId)).willReturn(false);
            given(workerRepoService.existsByPhoneNumber(phoneNumber)).willReturn(true);

            // when & then
            assertThrows(DuplicateResourceException.class,
                    () -> workerService.createWorker(request));
            verify(workerRepoService, never()).save(any(Worker.class));
        }

//        @Test
//        @DisplayName("이메일이 null인 작업자 생성")
//        void createWorker_WithNullEmail() {
//            // given
//            request.setEmail(null);
//            given(workerRepoService.existsByWorkerId(workerId)).willReturn(false);
//            given(workerRepoService.existsByPhoneNumber(phoneNumber)).willReturn(false);
//            given(zoneRepoService.findByZoneName(zoneName)).willReturn(zone);
//
//            // when
//            workerService.createWorker(request);
//
//            // then
//            verify(workerRepoService, times(1)).save(any(Worker.class));
//            verify(workerZoneRepoService, times(1)).save(any(WorkerZone.class));
//        }
//
//        @Test
//        @DisplayName("출입 가능 공간 목록이 비어있는 경우")
//        void createWorker_WithEmptyZoneNames() {
//            // given
//            request.setZoneNames(List.of());
//            given(workerRepoService.existsByWorkerId(workerId)).willReturn(false); // 존재하지 않음
//            given(workerRepoService.existsByPhoneNumber(phoneNumber)).willReturn(false); // 존재하지 않음
//            given(zoneRepoService.findByZoneName("대기실")).willReturn(waitRoom);
//
//            // when
//            workerService.createWorker(request); // 새로운 작업자 생성
//
//            // then
//            verify(workerRepoService, times(1)).save(any(Worker.class));
//            verify(workerZoneRepoService, never()).save(any(WorkerZone.class));
//        }
        /* ① 이메일이 null인 작업자 생성 */
        @Test
        void createWorker_WithNullEmail() {
            // given
            request.setEmail(null);
            given(workerRepoService.existsByWorkerId(workerId)).willReturn(false);
            given(workerRepoService.existsByPhoneNumber(phoneNumber)).willReturn(false);
            given(zoneRepoService.findByZoneName(zoneName)).willReturn(zone);
            given(zoneRepoService.findByZoneName("대기실")).willReturn(waitRoom);   // ← 추가

            // when
            workerService.createWorker(request);

            // then
            verify(workerRepoService, times(1)).save(any(Worker.class));
            verify(workerZoneRepoService, times(2)).save(any(WorkerZone.class));    // 1 → 2
        }

        /* ② zoneNames 가 비어 있는 경우 */
        @Test
        void createWorker_WithEmptyZoneNames() {
            // given
            request.setZoneNames(List.of());   // 불변 리스트
            given(workerRepoService.existsByWorkerId(workerId)).willReturn(false);
            given(workerRepoService.existsByPhoneNumber(phoneNumber)).willReturn(false);
            given(zoneRepoService.findByZoneName("대기실")).willReturn(waitRoom);   // ← 필수

            // when
            workerService.createWorker(request);

            // then
            verify(workerRepoService, times(1)).save(any(Worker.class));
            verify(workerZoneRepoService, times(1)).save(any(WorkerZone.class));    // never → 1
        }


    }

    @Nested
    @DisplayName("updateWorker 메서드 테스트")
    class UpdateWorkerTest {

        private CreateWorkerRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateWorkerRequest();
            request.setWorkerId(workerId);
            request.setName("새이름");
            request.setPhoneNumber("010-9999-8888");
            request.setEmail("new@test.com");
            request.setZoneNames(Arrays.asList(zoneName));
        }

        @Test
        @DisplayName("정상적인 작업자 정보 수정")
        void updateWorker_Success() {
            // given
            given(workerRepoService.findById(workerId)).willReturn(worker);
            given(workerRepoService.existsByPhoneNumber(request.getPhoneNumber())).willReturn(false);
            given(workerRepoService.existsByEmail(request.getEmail())).willReturn(false);
            given(workerZoneRepoService.findByWorker_WorkerId(workerId))
                    .willReturn(Arrays.asList(workerZone));
            given(zoneRepoService.findByZoneName(zoneName)).willReturn(zone);

            // when
            workerService.updateWorker(request);

            // then
            assertThat(worker.getName()).isEqualTo("새이름");
            assertThat(worker.getPhoneNumber()).isEqualTo("010-9999-8888");
            assertThat(worker.getEmail()).isEqualTo("new@test.com");
            verify(workerZoneRepoService, times(1)).deleteByWorkerWorkerId(workerId);
            verify(workerZoneRepoService, times(1)).save(any(WorkerZone.class));
        }

        @Test
        @DisplayName("중복된 전화번호로 수정 시도")
        void updateWorker_DuplicatePhoneNumber() {
            // given
            given(workerRepoService.findById(workerId)).willReturn(worker);
            given(workerRepoService.existsByPhoneNumber(request.getPhoneNumber())).willReturn(true);

            // when & then
            assertThrows(DuplicateResourceException.class,
                    () -> workerService.updateWorker(request));
        }

        @Test
        @DisplayName("중복된 이메일로 수정 시도")
        void updateWorker_DuplicateEmail() {
            // given
            given(workerRepoService.findById(workerId)).willReturn(worker);
            given(workerRepoService.existsByPhoneNumber(request.getPhoneNumber())).willReturn(false);
            given(workerRepoService.existsByEmail(request.getEmail())).willReturn(true);

            // when & then
            assertThrows(DuplicateResourceException.class,
                    () -> workerService.updateWorker(request));
        }

        @Test
        @DisplayName("이메일을 null로 수정")
        void updateWorker_WithNullEmail() {
            // given
            request.setEmail(null);
            given(workerRepoService.findById(workerId)).willReturn(worker);
            given(workerRepoService.existsByPhoneNumber(request.getPhoneNumber())).willReturn(false);
            given(workerZoneRepoService.findByWorker_WorkerId(workerId))
                    .willReturn(Arrays.asList(workerZone));
            given(zoneRepoService.findByZoneName(zoneName)).willReturn(zone);

            // when
            workerService.updateWorker(request);

            // then
            assertThat(worker.getEmail()).isNull();
            verify(workerZoneRepoService, times(1)).deleteByWorkerWorkerId(workerId);
            verify(workerZoneRepoService, times(1)).save(any(WorkerZone.class));
        }

        @Test
        @DisplayName("담당자 권한을 유지하면서 정보 수정")
        void updateWorker_KeepManagerRole() {
            // given
            WorkerZone managerZone = WorkerZone.builder()
                    .id(new WorkerZoneId(workerId, zoneId))
                    .worker(worker)
                    .zone(zone)
                    .manageYn(true)
                    .build();

            given(workerRepoService.findById(workerId)).willReturn(worker);
            given(workerRepoService.existsByPhoneNumber(request.getPhoneNumber())).willReturn(false);
            given(workerRepoService.existsByEmail(request.getEmail())).willReturn(false);
            given(workerZoneRepoService.findByWorker_WorkerId(workerId))
                    .willReturn(Arrays.asList(managerZone));
            given(zoneRepoService.findByZoneName(zoneName)).willReturn(zone);

            // when
            workerService.updateWorker(request);

            // then
            verify(workerZoneRepoService, times(1)).deleteByWorkerWorkerId(workerId);
            verify(workerZoneRepoService, times(1)).save(any(WorkerZone.class));
        }
    }

    @Test
    @DisplayName("현재 공간 담당자 조회 - 담당자가 있는 경우")
    void getCurrentManager_WhenManagerExists() {
        // given
        String zoneId = "20250507165750-827";  // zone1의 ID

        given(workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId))
                .willReturn(Optional.of(workerZone));

        // when
        WorkerCurrentLocationResponse manager = workerService.getZoneManager(zoneId);

        // then
        assertThat(manager).isNotNull();
        assertThat(manager.getWorkerId()).isEqualTo(worker.getWorkerId());
        assertThat(manager.getName()).isEqualTo(worker.getName());
        assertThat(manager.getEmail()).isEqualTo(worker.getEmail());
        assertThat(manager.getPhoneNumber()).isEqualTo(worker.getPhoneNumber());
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
        WorkerCurrentLocationResponse manager = workerService.getZoneManager(zoneId);

        // then
        assertThat(manager).isNull();
    }
} 