package com.factoreal.backend.domain.zone.application;

import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.dao.ZoneHistoryRepository;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZoneHistoryRepoService {
    private final ZoneHistoryRepository zoneHistoryRepository;
    /**
     * 특정 공간에 현재 들어가있는 작업자 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<ZoneHist> getCurrentWorkersByZoneId(String zoneId) {
        return zoneHistoryRepository.findByZone_ZoneIdAndExistFlag(zoneId, 1); // 해당 공간의 existFlag가 1인 모든 작업자 리스트
    }

    /**
     * 특정 작업자의 현재 위치 조회
     */
    @Transactional(readOnly = true)
    public ZoneHist getCurrentWorkerLocation(String workerId) {
        return zoneHistoryRepository.findByWorker_WorkerIdAndExistFlag(workerId, 1);
    }

    /**
     * 작업자ID와 작업자가 존재하는경우(1) 공간에 대한 기록을 반환하는 레포 접근 메서드
     */
    public ZoneHist findByWorker_WorkerIdAndExistFlag(String workerId, Integer ExistFlag) {
        return zoneHistoryRepository.findByWorker_WorkerIdAndExistFlag(workerId, ExistFlag);
    }

    /**
     * 공간에 대한 히스토리 저장하는 레포 접근 메서드
     */
    @Transactional
    protected ZoneHist save(ZoneHist zoneHist) {
        return zoneHistoryRepository.save(zoneHist);
    }

    /**
     * 작업자가 존재하고 작업중인 공간 히스토리를 조회하는 레포 접근 메서드
     * existFlag는 boolean 타입과 같은 개념으로 0 혹은 1이 들어옵니다.
     */
    public ZoneHist findByWorker_WorkerIdAndExistFlag(Worker worker, Integer existFlag) {
        return zoneHistoryRepository.findByWorker_WorkerIdAndExistFlag(worker.getWorkerId(), existFlag);
    }

    /**
     * 작업자가 존재하는 존재하는 공간 히스토리들을 조회
     * existFlag는 boolean 타입과 같은 개념으로 0 혹은 1이 들어옵니다.
     */
    public List<ZoneHist> findByZone_ZoneIdAndExistFlag(String zoneId, Integer existFlag) {
        return zoneHistoryRepository.findByZone_ZoneIdAndExistFlag(zoneId, existFlag);
    }
}
