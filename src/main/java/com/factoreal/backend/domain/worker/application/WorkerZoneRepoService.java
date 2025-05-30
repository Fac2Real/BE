package com.factoreal.backend.domain.worker.application;

import com.factoreal.backend.domain.worker.dao.WorkerZoneRepository;
import com.factoreal.backend.domain.worker.entity.WorkerZone;
import com.factoreal.backend.domain.worker.entity.WorkerZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerZoneRepoService {
    private final WorkerZoneRepository workerZoneRepository;

    // 특정 zone_id에 속한 작업자 목록 조회
    public List<WorkerZone> findByZoneZoneId(String zoneId) {
        return workerZoneRepository.findByZoneZoneId(zoneId);
    }

    /**
     * 존재하는 공간이고(공간Id가 있고) 관리자가 존재하는 공간의 작업자 공간 조회
     */
    // 특정 zone_id의 담당자 조회 (manageYn = true)
    public Optional<WorkerZone> findByZoneZoneIdAndManageYnIsTrue(String zoneId) {
        return workerZoneRepository.findByZoneZoneIdAndManageYnIsTrue(zoneId);
    }

    // 특정 공간을 제외한 다른 공간의 담당자 목록 조회
    public List<WorkerZone> findByZoneZoneIdNotAndManageYnIsTrue(String zoneId) {
        return workerZoneRepository.findByZoneZoneIdNotAndManageYnIsTrue(zoneId);
    }

    // 특정 작업자가 담당자로 있는 공간 조회
    public Optional<WorkerZone> findByWorkerWorkerIdAndManageYnIsTrue(String workerId) {
        return workerZoneRepository.findByWorkerWorkerIdAndManageYnIsTrue(workerId);
    }

    public Optional<WorkerZone> findById(WorkerZoneId workerZoneId) {
        return workerZoneRepository.findById(workerZoneId);
    }

    public WorkerZone save(WorkerZone workerZone) {
        return workerZoneRepository.save(workerZone);
    }

    public List<WorkerZone> findByWorker_WorkerId(String workerId) {
        return workerZoneRepository.findByWorker_WorkerId(workerId);
    }
}
