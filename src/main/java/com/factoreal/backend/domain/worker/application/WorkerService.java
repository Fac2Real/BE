package com.factoreal.backend.domain.worker.application;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;
import com.factoreal.backend.domain.worker.dto.request.CreateWorkerRequest;
import com.factoreal.backend.domain.worker.dto.response.WorkerDetailResponse;
import com.factoreal.backend.domain.worker.dto.response.WorkerInfoResponse;
import com.factoreal.backend.domain.worker.dto.response.ZoneManagerResponse;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.worker.entity.WorkerZone;
import com.factoreal.backend.domain.worker.entity.WorkerZoneId;
import com.factoreal.backend.domain.zone.application.ZoneHistoryRepoService;
import com.factoreal.backend.domain.zone.application.ZoneHistoryService;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {
    private final WorkerZoneRepoService workerZoneRepoService;
    private final WorkerRepoService workerRepoService;

    private final ZoneRepoService zoneRepoService;
    private final ZoneHistoryService zoneHistoryService;
    private final AbnormalLogService abnormalLogService;
    private final ZoneHistoryRepoService zoneHistoryRepoService;


    /**
     * 모든 작업자 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<WorkerDetailResponse> getAllWorkers() {
        log.info("전체 작업자 목록 조회");
        List<Worker> workers = workerRepoService.findAll();
        
        if (workers.isEmpty()) {
            log.info("등록된 작업자가 없습니다.");
            return new ArrayList<>();
        }
        
        // workerId 목록
        List<String> workerIds = workers.stream()
                .map(Worker::getWorkerId)
                .toList();

        // AbnormalLog 에서 작업자 상태 조회
        List<AbnormalLogResponse> statusList = abnormalLogService.
                findLatestAbnormalLogsForTargets(TargetType.Worker, workerIds);

        // 상태 Map<workerId, status>
        Map<String, Integer> statusMap = statusList.stream()
                .collect(
                        Collectors.toMap(
                                AbnormalLogResponse::getTargetId, 
                                AbnormalLogResponse::getDangerLevel
                                )
                        );

        // 위치 Map<workerId, zoneName>
        Map<String, Map<String, String>> zoneMap = workerIds.stream()
                .collect(Collectors.toMap(
                        workerId -> workerId,
                        workerId -> {
                            Map<String, String> zone = new HashMap<>();
                            try {
                                ZoneHist zh = zoneHistoryRepoService.findByWorker_WorkerIdAndExistFlag(workerId, 1);
                                if (zh != null && zh.getZone() != null) {
                                    zone.put("zoneId", zh.getZone().getZoneId());
                                    zone.put("zoneName", zh.getZone().getZoneName());
                                } else {
                                    zone.put("zoneId", "00000000000000-000");
                                    zone.put("zoneName", "대기실");
                                }
                            } catch (Exception e) {
                                log.error("작업자 위치 조회 중 오류 발생. workerId: {}, error: {}", workerId, e.getMessage());
                                zone.put("zoneId", "00000000000000-000");
                                zone.put("zoneName", "대기실");
                            }
                            return zone;
                        }
                ));

        return workers.stream()
                .map(worker -> {
                    try {
                        Map<String, String> zoneInfo = zoneMap.getOrDefault(worker.getWorkerId(), new HashMap<>());
                        return WorkerDetailResponse.fromEntity(
                                worker,
                                workerZoneRepoService.findByWorkerWorkerIdAndManageYnIsTrue(worker.getWorkerId())
                                        .isPresent(),
                                statusMap.getOrDefault(worker.getWorkerId(), 0),
                                zoneInfo.getOrDefault("zoneId", "00000000000000-000"),
                                zoneInfo.getOrDefault("zoneName", "대기실")
                        );
                    } catch (Exception e) {
                        log.error("작업자 정보 변환 중 오류 발생. worker: {}, error: {}", worker.getWorkerId(), e.getMessage());
                        return WorkerDetailResponse.fromEntity(
                                worker,
                                false,
                                0,
                                "00000000000000-000",
                                "대기실"
                        );
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 공간에 현재 들어가있는 작업자 목록 조회
     */
    public List<WorkerInfoResponse> getWorkersByZoneId(String zoneId) {
        log.info("공간 ID: {}의 현재 작업자 목록 조회", zoneId);
        // existFlag는 boolean 같은 개념 0 혹은 1이 들어감
        List<ZoneHist> currentWorkers = zoneHistoryRepoService.findByZone_ZoneIdAndExistFlag(zoneId, 1);
        return currentWorkers.stream()
                .map(zoneHist -> WorkerInfoResponse.from(zoneHist.getWorker(), false))
                .collect(Collectors.toList());
    }

    /**
     * 특정 공간의 담당자와 현재 위치 정보 조회
     */
    public ZoneManagerResponse getZoneManagerWithLocation(String zoneId) {
        log.info("공간 ID: {}의 담당자 정보 조회", zoneId);

        Optional<WorkerZone> zoneManager = workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId);
        if (zoneManager.isEmpty()) {
            return null;
        }

        Worker manager = zoneManager.get().getWorker();

        // 2. 담당자의 현재 위치 조회 (existFlag = 1)
        ZoneHist currentLocation = zoneHistoryRepoService.findByWorker_WorkerIdAndExistFlag(manager, 1);

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

        workerRepoService.save(worker); // 작업자 정보 저장

        // 2. 각 공간명으로 Zone 조회 및 WorkerZone 생성
        for (String zoneName : request.getZoneNames()) {
            Zone zone = zoneRepoService.findByZoneName(zoneName);

            // WorkerZone 생성 (기본적으로 관리자 권한은 없음)
            WorkerZone workerZone = WorkerZone.builder()
                    .id(new WorkerZoneId(worker.getWorkerId(), zone.getZoneId())) // 복합키 생성
                    .worker(worker)
                    .zone(zone)
                    .manageYn(false) // 담당자 권한은 없음이 default
                    .build();

            workerZoneRepoService.save(workerZone); // WorkerZone 저장
        }

        log.info("작업자 생성 완료 - workerId: {}", worker.getWorkerId());
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