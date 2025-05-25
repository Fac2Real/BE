package com.factoreal.backend.domain.zone.application;

import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.dao.ZoneHistoryRepository;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.domain.worker.dao.WorkerRepository;
import com.factoreal.backend.domain.zone.dao.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneHistoryService {
    
    private final ZoneHistoryRepository zoneHistRepository;
    private final WorkerRepository workerRepository;
    private final ZoneRepository zoneRepository;

    /**
     * 작업자의 위치 변경을 처리
     */
    @Transactional
    public void updateWorkerLocation(String workerId, String zoneId, LocalDateTime timestamp) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("작업자를 찾을 수 없습니다: " + workerId));
        
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("공간을 찾을 수 없습니다: " + zoneId));

        // 1. workerId 기반 작업자의 이전 위치가 있으면, 새로운 기록 생성 전 해당 작업자 위치 기록에 endTime 찍어주기
        ZoneHist currentLocation = zoneHistRepository.findByWorker_WorkerIdAndExistFlag(workerId, 1);
        if (currentLocation != null) {
            currentLocation.setEndTime(timestamp); // 다음 공간의 입장 시간으로 update
            currentLocation.setExistFlag(0);
            zoneHistRepository.save(currentLocation);
        }

        // 2. 새로운 위치 기록 생성
        ZoneHist newLocation = ZoneHist.builder()
                .worker(worker)
                .zone(zone)
                .startTime(timestamp)
                .endTime(null)
                .existFlag(1)
                .build();
        
        zoneHistRepository.save(newLocation);
        
        /**
         * currentLocation이 있으면 (이전 위치가 있으면) -> 그 공간의 ID를 출력
         * currentLocation이 없으면 (최초 입장이면) -> "없음" 출력
         */
        log.info("작업자 {} 위치 변경: {} -> {}", workerId, 
                currentLocation != null ? currentLocation.getZone().getZoneId() : "없음", 
                zoneId);
    }

    public ZoneHist findByWorker_WorkerIdAndExistFlag(String workerid, int existFlag) {
        return zoneHistRepository.findByWorker_WorkerIdAndExistFlag(workerid,existFlag);
    }


}