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
import com.factoreal.backend.domain.zone.dto.response.ZoneInfoResponse;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.global.exception.dto.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {
    private final WorkerZoneRepoService workerZoneRepoService;
    private final WorkerRepoService workerRepoService;

    private final ZoneRepoService zoneRepoService;
    private final AbnormalLogService abnormalLogService;
    private final ZoneHistoryRepoService zoneHistoryRepoService;
    private final ZoneHistoryService zoneHistoryService;


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
                        // 잭슨 직렬화 문제로 ZoneInfo DTO 필요함 -> Zone 엔티티를 직접 사용 불가
                        // 1) 출입 권한 zone 전부
                        List<ZoneInfoResponse> accessZones = workerZoneRepoService
                                .findByWorker_WorkerId(worker.getWorkerId())  // List<WorkerZone>
                                .stream()
                                .map(wz -> new ZoneInfoResponse(
                                        wz.getZone().getZoneId(),
                                        wz.getZone().getZoneName()))
                                .toList();

                        // 2) 담당 zone (있으면 1개) → List<Zone>
                        List<ZoneInfoResponse> managedZones = workerZoneRepoService
                                .findByWorkerWorkerIdAndManageYnIsTrue(worker.getWorkerId())
                                .map(wz -> List.of(new ZoneInfoResponse(
                                        wz.getZone().getZoneId(),
                                        wz.getZone().getZoneName())))
                                .orElse(List.of());
                        Map<String, String> zoneInfo = zoneMap.getOrDefault(worker.getWorkerId(), new HashMap<>());
                        return WorkerDetailResponse.fromEntity(
                                worker,
                                workerZoneRepoService.findByWorkerWorkerIdAndManageYnIsTrue(worker.getWorkerId())
                                        .isPresent(),
                                statusMap.getOrDefault(worker.getWorkerId(), 0),
                                zoneInfo.getOrDefault("zoneId", "00000000000000-000"),
                                zoneInfo.getOrDefault("zoneName", "대기실"),
                                accessZones,
                                managedZones
                        );
                    } catch (Exception e) {
                        log.error("작업자 정보 변환 중 오류 발생. worker: {}, error: {}", worker.getWorkerId(), e.getMessage());
                        return WorkerDetailResponse.fromEntity(
                                worker,
                                false,
                                0,
                                "00000000000000-000",
                                "대기실",
                                List.of(), List.of()
                        );
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 공간에 현재 들어가있는 작업자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<WorkerInfoResponse> getWorkersByZoneId(String zoneId) {
        log.info("공간 ID: {}의 현재 작업자 목록 조회", zoneId);

        // 1. 해당 zoneId에 존재하는 작업자 목록 조회
        List<ZoneHist> currentWorkers = zoneHistoryRepoService.findByZone_ZoneIdAndExistFlag(zoneId, 1);

        // 2. 조회된 작업자들의 ID 목록 추출
        List<String> workerIds = currentWorkers.stream()
                .map(zoneHist -> zoneHist.getWorker().getWorkerId())
                .toList();

        // 2-2. 해당 zone의 담당자 ID가 작업자 ID와 같은지 확인
        Optional<WorkerZone> zoneManager = workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId);
        String managerId = zoneManager.map(wz -> wz.getWorker().getWorkerId()).orElse(null);

        // 3. AbnormalLog 에서 작업자 상태 조회
        List<AbnormalLogResponse> statusList = abnormalLogService.
                findLatestAbnormalLogsForTargets(TargetType.Worker, workerIds);

        // 4. 상태 Map<workerId, status>
        Map<String, Integer> statusMap = statusList.stream()
                .collect(
                        Collectors.toMap(
                                AbnormalLogResponse::getTargetId,
                                AbnormalLogResponse::getDangerLevel
                        ));

        return currentWorkers.stream()
                .map(zoneHist -> {
                    Worker worker = zoneHist.getWorker();
                    Integer status = statusMap.getOrDefault(worker.getWorkerId(), 0);
                    Boolean isManager = Objects.equals(managerId, worker.getWorkerId());
                    return WorkerInfoResponse.from(worker, isManager, status);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 공간의 담당자와 현재 위치 정보 조회
     */
    @Transactional(readOnly = true)
    public WorkerCurrentLocationResponse getZoneManager(String zoneId) {
        log.info("공간 ID: {}의 담당자 정보 조회", zoneId);

        // 1. 담당자 조회
        Optional<WorkerZone> workerZone = workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId);
        if (workerZone.isEmpty()) { return null; }

        Worker manager = workerZone.get().getWorker();

        // 2. 담당자 상태 조회
        List<AbnormalLogResponse> statusList = abnormalLogService.findLatestAbnormalLogsForTargets(
                TargetType.Worker,
                Collections.singletonList(manager.getWorkerId())
        );

        Integer status = statusList.stream()
                .findFirst()
                .map(AbnormalLogResponse::getDangerLevel)
                .orElse(0);

        // 3. 담당자의 현재 위치 조회 (existFlag = 1)
        ZoneHist currentLocation = zoneHistoryRepoService.findByWorker_WorkerIdAndExistFlag(manager, 1);

        // 4. 현재 위치한 공간 정보 (없을 수 있음)
        Zone currentZone = currentLocation != null ? currentLocation.getZone() : null;

        return WorkerCurrentLocationResponse.from(manager, currentZone, true, status);
    }

    /**
     * 작업자 생성 및 출입 가능 공간 설정
     */
    @Transactional
    public void createWorker(CreateWorkerRequest request) {
        log.info("작업자 생성 요청: {}", request);

        // 등록할때 중복되는 작업자의 경우 반려시키는 로직 추가
        if (workerRepoService.existsByWorkerId(request.getWorkerId())) {
            log.info("이미 존재하는 작업자입니다.(작업자ID 중복)");
            throw new DuplicateResourceException("이미 존재하는 작업자입니다.(작업자ID 중복)");
        }
        if (workerRepoService.existsByPhoneNumber(request.getPhoneNumber())) {
            log.info("이미 존재하는 작업자입니다.(작업자ID 중복)");
            throw new DuplicateResourceException("이미 존재하는 작업자입니다.(전화번호 중복).");
        }

        // 1. 작업자 정보 저장
        Worker worker = Worker.builder()
                .workerId(request.getWorkerId())
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .build();

        workerRepoService.save(worker); // 작업자 정보 저장

        /* 1. zoneNames 리스트를 생성해서 안전한 로직으로 추가하기 */
        List<String> zoneNames = new ArrayList<>(
                Optional.ofNullable(request.getZoneNames()).orElseGet(ArrayList::new)
        );
        if (!zoneNames.contains("대기실")) {
            zoneNames.add("대기실");
        }

        request.setZoneNames(zoneNames);

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
        // 3. 기본적으로 작업자를 대기실로 배정하기 (작업자 , 대기실, 작업자를 등록하는 시간대로) 등록하기
        zoneHistoryService.updateWorkerLocation(worker.getWorkerId(),"00000000000000-000", LocalDateTime.now());

        log.info("작업자 생성 완료 - workerId: {}", worker.getWorkerId());
    }

    @Transactional
    public void updateWorker(CreateWorkerRequest r) {

        /* 1) 대상 작업자 조회 */
        Worker worker = workerRepoService.findById(r.getWorkerId());

        /* 2) 전화번호 중복 검사 (자기 자신 제외) */
        if (!worker.getPhoneNumber().equals(r.getPhoneNumber())
                && workerRepoService.existsByPhoneNumber(r.getPhoneNumber()))
            throw new DuplicateResourceException("중복되는 휴대폰번호입니다.");

        if (!worker.getEmail().equals(r.getEmail()) &&
                workerRepoService.existsByEmail(r.getEmail()))
            throw new DuplicateResourceException("중복되는 이메일입니다.");
        /* 3) 기본정보 수정 */
        worker.setName(r.getName());
        worker.setPhoneNumber(r.getPhoneNumber());
        worker.setEmail(r.getEmail());

        /* 4) 출입-권한(WorkerZone) 재설정 -------------- */
        // 4-1. 현재 담당자(manageYn=true)인 공간 KEEP
        List<WorkerZone> managerZones = workerZoneRepoService
                .findByWorker_WorkerId(worker.getWorkerId())
                .stream()
                .filter(WorkerZone::getManageYn)
                .toList();                    // 담장 구역은 그대로 유지

        // 4-2. 기존 권한 전부 삭제
        workerZoneRepoService.deleteByWorkerWorkerId(worker.getWorkerId());

        // 4-3. 요청한 zoneNames + managerZones를 합쳐 다시 저장
        //      (중복은 Set 으로 제거)
        Set<String> newZoneNames = new HashSet<>(r.getZoneNames());
        managerZones.forEach(mz -> newZoneNames.add(mz.getZone().getZoneName())); // keep

        for (String zoneName : newZoneNames) {
            Zone zone = zoneRepoService.findByZoneName(zoneName);       // 400 발생 가능 ⇒ 글로벌 예외 처리 권장
            boolean isManager = managerZones.stream()
                    .anyMatch(mz -> mz.getZone().getZoneId().equals(zone.getZoneId()));

            WorkerZone wz = WorkerZone.builder()
                    .id(new WorkerZoneId(worker.getWorkerId(), zone.getZoneId()))
                    .worker(worker)
                    .zone(zone)
                    .manageYn(isManager)
                    .build();
            workerZoneRepoService.save(wz);
        }
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