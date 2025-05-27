package com.factoreal.backend.domain.worker.application;

import com.factoreal.backend.domain.worker.dto.request.CreateWorkerRequest;
import com.factoreal.backend.domain.worker.dto.response.WorkerDetailResponse;
import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;
import com.factoreal.backend.domain.worker.dto.response.WorkerInfoResponse;
import com.factoreal.backend.domain.worker.dto.response.ZoneManagerResponse;
import com.factoreal.backend.domain.zone.application.ZoneHistoryService;
import com.factoreal.backend.domain.zone.dao.ZoneHistoryRepository;
import com.factoreal.backend.domain.zone.dao.ZoneRepository;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.worker.entity.WorkerZone;
import com.factoreal.backend.domain.worker.entity.WorkerZoneId;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.domain.worker.dao.WorkerRepository;
import com.factoreal.backend.domain.worker.dao.WorkerZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {
    private final WorkerRepository workerRepository;
    private final WorkerZoneService workerZoneService;


    private final ZoneRepository zoneRepository;
    private final ZoneHistoryService zoneHistoryService;
    private final AbnormalLogService abnormalLogService;
    private final ZoneHistoryRepository zoneHistoryRepository;


    /**
     * 모든 작업자 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<WorkerDetailResponse> getAllWorkers() {
        log.info("전체 작업자 목록 조회");
        List<Worker> workers = findAll();
        // workerId 목록
        List<String> workerIds = workers.stream()
                .map(Worker::getWorkerId)
                .toList();

        // AbnormalLog 에서 작업자 상태 조회
        List<AbnormalLogResponse> statusList = abnormalLogService.
                findLatestAbnormalLogsForTargets(TargetType.Worker, workerIds);
        // HistZone에서 작업자 위치 조회
//        List<ZoneHist> zoneHistsList = workerIds.stream()
//                .map(workerId -> zoneHistoryRepository.findByWorker_WorkerIdAndExistFlag(workerId, 1))
//                .toList();

        // 상태 Map<workerId, status>
        Map<String, Integer> statusMap = statusList.stream()
                .collect(Collectors.toMap(AbnormalLogResponse::getTargetId, AbnormalLogResponse::getDangerLevel));

        // 위치 Map<workerId, zoneName>
        Map<String, Map<String, String>> zoneMap = workerIds.stream()
                .collect(Collectors.toMap(
                        workerId -> workerId,
                        workerId -> {
                            ZoneHist zh = zoneHistoryRepository.findByWorker_WorkerIdAndExistFlag(workerId, 1);
                            if (zh == null || zh.getZone() == null) {
                                return new HashMap<>();
                            }
                            Map<String, String> zone = new HashMap<>();
                            zone.put("zoneId", zh.getZone().getZoneId());
                            zone.put("zoneName", zh.getZone().getZoneName());
                            return zone;
                        }
                ));
        return workers.stream()
                .map(worker -> WorkerDetailResponse.fromEntity(
                        worker,
                        workerZoneService.findByWorkerWorkerIdAndManageYnIsTrue(worker.getWorkerId())
                                .isPresent(),
                        statusMap.getOrDefault(worker.getWorkerId(),0),
                        zoneMap.get(worker.getWorkerId()).getOrDefault("zoneId","00000000000000-000"),
                        zoneMap.get(worker.getWorkerId()).getOrDefault("zoneName","대기실")
                ))
                .collect(Collectors.toList());
    }

    /**
     * 특정 공간에 현재 들어가있는 작업자 목록 조회
     */
    public List<WorkerInfoResponse> getWorkersByZoneId(String zoneId) {
        log.info("공간 ID: {}의 현재 작업자 목록 조회", zoneId);
        // existFlag는 boolean 같은 개념 0 혹은 1이 들어감
        List<ZoneHist> currentWorkers = findByZone_ZoneIdAndExistFlag(zoneId, 1);
        return currentWorkers.stream()
                .map(zoneHist -> WorkerInfoResponse.from(zoneHist.getWorker(), false))
                .collect(Collectors.toList());
    }

    /**
     * 특정 공간의 담당자와 현재 위치 정보 조회
     */
    public ZoneManagerResponse getZoneManagerWithLocation(String zoneId) {
        log.info("공간 ID: {}의 담당자 정보 조회", zoneId);

        WorkerZone zoneManager = findByZoneZoneIdAndManageYnIsTrue(zoneId);

        Worker manager = zoneManager.getWorker();

        // 2. 담당자의 현재 위치 조회 (existFlag = 1)
        ZoneHist currentLocation = findByWorker_WorkerIdAndExistFlag(manager, 1);

        // 3. 현재 위치한 공간 정보 (없을 수 있음)
        Zone currentZone = currentLocation != null ? currentLocation.getZone() : null;

        return ZoneManagerResponse.from(manager, currentZone);
    }

    /**
     * 작업자 생성 및 출입 가능 공간 설정
     */
    @Transactional
    public void createWorker(CreateWorkerRequest request) {
        log.info("작업자 생성 요청: {}", request);

        // 1. 작업자 정보 저장
        Worker worker = Worker.builder()
                .workerId(request.getWorkerId())
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .build();

        workerRepository.save(worker); // 작업자 정보 저장

        // 2. 각 공간명으로 Zone 조회 및 WorkerZone 생성
        for (String zoneName : request.getZoneNames()) {
            Zone zone = zoneRepository.findByZoneName(zoneName)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공간명입니다: " + zoneName));

            // WorkerZone 생성 (기본적으로 관리자 권한은 없음)
            WorkerZone workerZone = WorkerZone.builder()
                    .id(new WorkerZoneId(worker.getWorkerId(), zone.getZoneId())) // 복합키 생성
                    .worker(worker)
                    .zone(zone)
                    .manageYn(false) // 담당자 권한은 없음이 default
                    .build();

            workerZoneService.save(workerZone); // WorkerZone 저장
        }

        log.info("작업자 생성 완료 - workerId: {}", worker.getWorkerId());
    }

    /**
     * workerId에 해당하는 작업자 조회
     */
    @Transactional(readOnly = true)
    public Worker getWorkerByWorkerId(String workerId) {
        return workerRepository.findById(workerId).orElseThrow();
    }

    /**
     * FCM 발송용 토큰을 추가하기 위한 메서드
     *
     * @param worker
     * @return
     */
    @Transactional
    public Worker saveWorker(Worker worker) {
        return workerRepository.save(worker);
    }

    /**
     * 존재하는 공간이고(공간Id가 있고) 관리자가 존재하는 공간의 작업자 공간 조회
     */
    private WorkerZone findByZoneZoneIdAndManageYnIsTrue(String zoneId) {
        return workerZoneService.findByZoneZoneIdAndManageYnIsTrue(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공간의 담당자를 찾을 수 없습니다: " + zoneId));
    }

    /**
     * 작업자가 존재하고 작업중인 공간 히스토리를 조회하는 레포 접근 메서드
     * existFlag는 boolean 타입과 같은 개념으로 0 혹은 1이 들어옵니다.
     */
    private ZoneHist findByWorker_WorkerIdAndExistFlag(Worker worker, Integer existFlag) {
        return zoneHistoryRepository.findByWorker_WorkerIdAndExistFlag(worker.getWorkerId(), existFlag);
    }

    /**
     * 작업자가 존재하는 존재하는 공간 히스토리들을 조회
     * existFlag는 boolean 타입과 같은 개념으로 0 혹은 1이 들어옵니다.
     */
    private List<ZoneHist> findByZone_ZoneIdAndExistFlag(String zoneId, Integer existFlag) {
        return zoneHistoryRepository.findByZone_ZoneIdAndExistFlag(zoneId, existFlag);
    }

    /**
     * 모든 작업자 리스트를 조회하는 레포 접근 메서드
     */
    private List<Worker> findAll() {
        return workerRepository.findAll();
    }

//    public Worker findById(String workerId) {
//        return workerRepository.findById(workerId)
//                .orElseThrow(() -> new IllegalArgumentException("작업자를 찾을 수 없습니다: " + workerId));
//    }
}

// TODO. 수정되어야 할 로직. 현재는 WorkerZone 테이블에서 공간id로 필터링 되는 모든 작업자를 끌고왔는데,
// 사실 현재 그 공간에서 실제로 작업하고 있는, 즉 들어
// public List<WorkerDto> getWorkersByZoneId(String zoneId) {
//     log.info("공간 ID: {}의 작업자 목록 조회", zoneId);
//     List<WorkerZone> workerZones = workerZoneRepository.findByZoneZoneId(zoneId);

//     return workerZones.stream()
//             .map(workerZone -> {
//                 Worker worker = workerZone.getWorker();
//                 Boolean isManager = workerZone.getManageYn();
//                 return WorkerDto.fromEntity(worker, isManager);
//             })
//             .collect(Collectors.toList());
// }