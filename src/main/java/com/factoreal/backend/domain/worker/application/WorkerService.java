package com.factoreal.backend.domain.worker.application;

import com.factoreal.backend.domain.worker.dto.request.CreateWorkerRequest;
import com.factoreal.backend.domain.worker.dto.response.WorkerDetailResponse;
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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {
    private final WorkerRepository workerRepository;
    private final WorkerZoneRepository workerZoneRepository;
    private final ZoneRepository zoneRepository;
    private final ZoneHistoryService zoneHistoryService;

    private final ZoneHistoryRepository zoneHistoryRepository;

    @Transactional(readOnly = true)
    public List<WorkerDetailResponse> getAllWorkers() {
        log.info("전체 작업자 목록 조회");
        List<Worker> workers = workerRepository.findAll();
        return workers.stream()
                .map(worker -> {
                    // 해당 작업자가 어떤 공간의 담당자인지 확인
                    boolean isManager = workerZoneRepository.findByWorkerWorkerIdAndManageYnIsTrue(worker.getWorkerId())
                            .isPresent();

                    // 작업자의 현재 위치 정보 조회
                    ZoneHist currentLocation = zoneHistoryService.getCurrentWorkerLocation(worker.getWorkerId());

                    // 작업자 상태와 위치 정보 초기 설정
                    // TODO: 작업자 상태 추가 필요
                    String status = "Stable";
                    String currentZoneId = null;
                    String currentZoneName = null;

                    if (currentLocation != null) {
                        Zone currentZone = currentLocation.getZone();
                        currentZoneId = currentZone.getZoneId();
                        currentZoneName = currentZone.getZoneName();
                    }

                    return WorkerDetailResponse.fromEntity(worker, isManager, status, currentZoneId, currentZoneName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 공간에 현재 들어가있는 작업자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<WorkerInfoResponse> getWorkersByZoneId(String zoneId) {
        log.info("공간 ID: {}의 현재 작업자 목록 조회", zoneId);
        List<ZoneHist> currentWorkers = zoneHistoryRepository.findByZone_ZoneIdAndExistFlag(zoneId, 1);
        return currentWorkers.stream()
                .map(zoneHist -> WorkerInfoResponse.from(zoneHist.getWorker(), false))
                .collect(Collectors.toList());
    }

    /**
     * 특정 공간의 담당자와 현재 위치 정보 조회
     */
    @Transactional(readOnly = true)
    public ZoneManagerResponse getZoneManagerWithLocation(String zoneId) {
        log.info("공간 ID: {}의 담당자 정보 조회", zoneId);

        WorkerZone zoneManager = workerZoneRepository.findByZoneZoneIdAndManageYnIsTrue(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공간의 담당자를 찾을 수 없습니다: " + zoneId));

        Worker manager = zoneManager.getWorker();

        // 2. 담당자의 현재 위치 조회 (existFlag = 1)
        ZoneHist currentLocation = zoneHistoryRepository.findByWorker_WorkerIdAndExistFlag(manager.getWorkerId(), 1);

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

            workerZoneRepository.save(workerZone); // WorkerZone 저장
        }

        log.info("작업자 생성 완료 - workerId: {}", worker.getWorkerId());
    }
}
