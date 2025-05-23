package com.factoreal.backend.domain.worker.application;

import com.factoreal.backend.domain.worker.dto.WorkerDto;
import com.factoreal.backend.domain.zone.application.ZoneHistoryService;
import com.factoreal.backend.domain.zone.dto.ZoneManagerResponseDto;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.workerZone.entity.WorkerZone;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.domain.worker.dao.WorkerRepository;
import com.factoreal.backend.domain.workerZone.dao.WorkerZoneRepository;
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
    private final ZoneHistoryService zoneHistoryService;
    private final WorkerZoneRepository workerZoneRepository;

    @Transactional(readOnly = true)
    public List<WorkerDto> getAllWorkers() {
        log.info("전체 작업자 목록 조회");
        List<Worker> workers = workerRepository.findAll();
        return workers.stream()
                .map(worker -> WorkerDto.fromEntity(worker, false))
                .collect(Collectors.toList());
    }

    /**
     * 특정 공간에 현재 들어가있는 작업자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<WorkerDto> getWorkersByZoneId(String zoneId) {
        log.info("공간 ID: {}의 현재 작업자 목록 조회", zoneId);
        
        // 현재 해당 공간에 있는 작업자 이력 조회 (existFlag = 1)
        List<ZoneHist> currentWorkers = zoneHistoryService.getCurrentWorkersByZoneId(zoneId);
        
        // ZoneHist에서 Worker 정보만 추출하여 DTO로 변환
        return currentWorkers.stream()
                .map(zoneHist -> WorkerDto.fromEntity(zoneHist.getWorker(), false))
                .collect(Collectors.toList());
    }

    /**
     * 특정 공간의 담당자와 현재 위치 정보 조회
     */
    @Transactional(readOnly = true)
    public ZoneManagerResponseDto getZoneManagerWithLocation(String zoneId) {
        log.info("공간 ID: {}의 담당자 정보 조회", zoneId);
        
        // 1. 해당 공간의 담당자 조회 (manageYn = true)
        WorkerZone zoneManager = workerZoneRepository.findByZoneZoneIdAndManageYnIsTrue(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공간의 담당자를 찾을 수 없습니다: " + zoneId));
        
        Worker manager = zoneManager.getWorker();
        
        // 2. 담당자의 현재 위치 조회 (existFlag = 1)
        ZoneHist currentLocation = zoneHistoryService.getCurrentWorkerLocation(manager.getWorkerId());
        
        // 3. 현재 위치한 공간 정보 (없을 수 있음)
        Zone currentZone = currentLocation != null ? currentLocation.getZone() : null;
        
        // 4. DTO 변환 및 반환
        return ZoneManagerResponseDto.fromEntity(manager, currentZone);
    }
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