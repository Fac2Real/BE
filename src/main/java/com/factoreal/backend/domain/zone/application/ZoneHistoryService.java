package com.factoreal.backend.domain.zone.application;

import com.factoreal.backend.domain.state.store.InMemoryZoneWorkerStateStore;
import com.factoreal.backend.domain.worker.application.WorkerRepoService;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneHistoryService {

    private final InMemoryZoneWorkerStateStore zoneWorkerStateStore;
    private final ZoneHistoryRepoService zoneHistoryRepoService;
    private final ZoneRepoService zoneRepoService;
    private final WorkerRepoService workerRepoService;

    /**
     * 작업자의 위치 변경을 처리
     */
    @Transactional
    public void updateWorkerLocation(String workerId, String zoneId, LocalDateTime timestamp) {
        // 0. 작업자에 대한 락 선점
        Worker lockedWorker = workerRepoService.lockWorkerRow(workerId);  // 락 획득

        // 1. State 변경 (메모리 상태)
        zoneWorkerStateStore.setWorkerRiskLevel(zoneId, workerId, zoneWorkerStateStore.getWorkerRiskLevel(workerId));

        // 2. 이전 위치 종료 처리
        ZoneHist currentLocation = zoneHistoryRepoService.findByWorkerLocateForUpdate(workerId);
        if (currentLocation != null) {
            currentLocation.setEndTime(timestamp);
            currentLocation.setExistFlag(0);
            zoneHistoryRepoService.save(currentLocation);
        }

        // 3. 새로운 위치 기록
        Zone zone = zoneRepoService.findById(zoneId);
        ZoneHist newLocation = ZoneHist.builder()
            .worker(lockedWorker)  // 이미 락 잡힌 worker 사용
            .zone(zone)
            .startTime(timestamp)
            .endTime(null)
            .existFlag(1)
            .build();
        zoneHistoryRepoService.save(newLocation);

        // 4. 로그 출력
        log.info("작업자 {} 위치 변경: {} -> {}", workerId,
            currentLocation != null ? currentLocation.getZone().getZoneId() : "없음",
            zoneId);
    }
}