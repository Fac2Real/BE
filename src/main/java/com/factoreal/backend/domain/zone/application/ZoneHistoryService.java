package com.factoreal.backend.domain.zone.application;

import com.factoreal.backend.domain.worker.application.WorkerRepoService;
import com.factoreal.backend.domain.worker.application.WorkerService;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.dao.ZoneHistoryRepository;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneHistoryService {

    private final ZoneHistoryRepository zoneHistRepository;
//    private final WorkerService workerService;
//    private final ZoneService zoneService;
    private final ZoneRepoService zoneRepoService;
    private final WorkerRepoService workerRepoService;

    /**
     * 작업자의 위치 변경을 처리
     */
    @Transactional
    public void updateWorkerLocation(String workerId, String zoneId, LocalDateTime timestamp) {
        Worker worker = workerRepoService.findById(workerId);

        Zone zone = zoneRepoService.findById(zoneId);

        // 1. workerId 기반 작업자의 이전 위치가 있으면, 새로운 기록 생성 전 해당 작업자 위치 기록에 endTime 찍어주기
        ZoneHist currentLocation = findByWorker_WorkerIdAndExistFlag(workerId, 1);
        if (currentLocation != null) {
            currentLocation.setEndTime(timestamp); // 다음 공간의 입장 시간으로 update
            currentLocation.setExistFlag(0);
            save(currentLocation);
        }

        // 2. 새로운 위치 기록 생성
        ZoneHist newLocation = ZoneHist.builder()
                .worker(worker)
                .zone(zone)
                .startTime(timestamp)
                .endTime(null)
                .existFlag(1)
                .build();

        save(newLocation);

        /**
         * currentLocation이 있으면 (이전 위치가 있으면) -> 그 공간의 ID를 출력
         * currentLocation이 없으면 (최초 입장이면) -> "없음" 출력
         */
        log.info("작업자 {} 위치 변경: {} -> {}", workerId,
                currentLocation != null ? currentLocation.getZone().getZoneId() : "없음",
                zoneId);
    }

    /**
     * 특정 공간에 현재 들어가있는 작업자 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<ZoneHist> getCurrentWorkersByZoneId(String zoneId) {
        return zoneHistRepository.findByZone_ZoneIdAndExistFlag(zoneId, 1); // 해당 공간의 existFlag가 1인 모든 작업자 리스트
    }

    /**
     * 특정 작업자의 현재 위치 조회
     */
    @Transactional(readOnly = true)
    public ZoneHist getCurrentWorkerLocation(String workerId) {
        return zoneHistRepository.findByWorker_WorkerIdAndExistFlag(workerId, 1);
    }

    /**
     * 작업자ID와 작업자가 존재하는경우(1) 공간에 대한 기록을 반환하는 레포 접근 메서드
     */
    private ZoneHist findByWorker_WorkerIdAndExistFlag(String workerId, Integer ExistFlag) {
        return zoneHistRepository.findByWorker_WorkerIdAndExistFlag(workerId, ExistFlag);
    }

    /**
     * 공간에 대한 히스토리 저장하는 레포 접근 메서드
     */
    @Transactional
    protected ZoneHist save(ZoneHist zoneHist) {
        return zoneHistRepository.save(zoneHist);
    }
}