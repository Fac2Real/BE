package com.factoreal.backend.domain.zone.application;

import com.factoreal.backend.domain.state.store.InMemoryZoneWorkerStateStore;
import com.factoreal.backend.domain.worker.application.WorkerRepoService;
import com.factoreal.backend.domain.worker.application.WorkerZoneRepoService;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.worker.entity.WorkerZone;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.global.exception.dto.BadRequestException;
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

    private final InMemoryZoneWorkerStateStore zoneWorkerStateStore;
    private final ZoneHistoryRepoService zoneHistoryRepoService;
    private final ZoneRepoService zoneRepoService;
    private final WorkerRepoService workerRepoService;
    private final WorkerZoneRepoService workerZoneRepoService;

    /**
     * 작업자의 위치 변경을 처리
     */
    @Transactional
    public void updateWorkerLocation(String workerId, String zoneId, LocalDateTime timestamp) {
        // 0. State 변경
        zoneWorkerStateStore.setWorkerRiskLevel(zoneId, workerId, zoneWorkerStateStore.getWorkerRiskLevel(workerId));

        // 0.5 작업자가 해당 zone 출입 권한을 가지고 있는지 확인
        List<WorkerZone> availableZones = workerZoneRepoService.findByWorker_WorkerId(workerId);
        // WorkerZone > zone 의 Id와 zoneId와 매치되는지 검증
        boolean isZoneValid = availableZones.stream()
                .anyMatch(workerZone ->
                        workerZone.getZone() != null && // Zone 객체가 null이 아닌지 안전하게 확인
                                workerZone.getZone().getZoneId().equals(zoneId)
                );

        if (!isZoneValid) {
            throw new BadRequestException(String.format("작업자 ID: %s는 공간 ID: %s에 대한 접근 권한이 없습니다.", workerId, zoneId));
        }


        // 1. workerId 기반 작업자의 이전 위치가 있으면, 새로운 기록 생성 전 해당 작업자 위치 기록에 endTime 찍어주기
        ZoneHist currentLocation = zoneHistoryRepoService.getCurrentWorkerLocation(workerId);
        if (currentLocation != null) {
            currentLocation.setEndTime(timestamp); // 다음 공간의 입장 시간으로 update
            currentLocation.setExistFlag(0);
            zoneHistoryRepoService.save(currentLocation);
        }

        // 2. 새로운 위치 기록 생성
        Worker worker = workerRepoService.findById(workerId);
        Zone zone = zoneRepoService.findById(zoneId);
        ZoneHist newLocation = ZoneHist.builder()
                .worker(worker)
                .zone(zone)
                .startTime(timestamp)
                .endTime(null)
                .existFlag(1)
                .build();

        zoneHistoryRepoService.save(newLocation);

        /**
         * currentLocation이 있으면 (이전 위치가 있으면) -> 그 공간의 ID를 출력
         * currentLocation이 없으면 (최초 입장이면) -> "없음" 출력
         */
        log.info("작업자 {} 위치 변경: {} -> {}", workerId,
                currentLocation != null ? currentLocation.getZone().getZoneId() : "없음",
                zoneId);
    }
}