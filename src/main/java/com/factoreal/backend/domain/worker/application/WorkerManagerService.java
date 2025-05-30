package com.factoreal.backend.domain.worker.application;

import com.factoreal.backend.domain.worker.dto.response.WorkerManagerResponse;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.worker.entity.WorkerZone;
import com.factoreal.backend.domain.worker.entity.WorkerZoneId;
import com.factoreal.backend.global.exception.dto.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
// 공간 담당자 지정 서비스
public class WorkerManagerService {

    private final WorkerZoneRepoService workerZoneRepoService;

    /**
     * 특정 공간의 담당자 후보 목록 조회
     * - 이미 담당자가 있는 경우: 현재 담당자를 제외한 해당 공간 접근 권한이 있는 작업자 목록
     * - 담당자가 없는 경우: 해당 공간 접근 권한이 있는 작업자 목록
     * - 다른 공간의 담당자인 작업자는 후보 목록에서 제외
     */
    @Transactional(readOnly = true)
    public List<WorkerManagerResponse> getManagerCandidates(String zoneId) {
        log.info("공간 ID: {}의 담당자 후보 목록 조회", zoneId);

        // 1. 현재 해당 공간의 담당자 조회
        Optional<WorkerZone> currentManager = workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId);

        // 2. 현재 공간을 제외한 다른 공간의 담당자 목록 조회 (workerId를 Set으로 묶어서 중복 제거)
        Set<String> otherManagerIds = workerZoneRepoService.findByZoneZoneIdNotAndManageYnIsTrue(zoneId).stream()
                .map(wz -> wz.getWorker().getWorkerId())
                .collect(Collectors.toSet());

        // 3. 해당 공간에 접근 권한이 있는 작업자 목록 조회
        List<WorkerZone> zoneWorkers = workerZoneRepoService.findByZoneZoneId(zoneId);

        // 4. 현재 담당자와 다른 공간의 담당자를 제외한 후보 목록 생성
        List<Worker> candidates = currentManager
                .map(manager -> zoneWorkers.stream()
                        .filter(wz -> !wz.getWorker().getWorkerId().equals(manager.getWorker().getWorkerId())) // 현재 담당자를 제외
                        .filter(wz -> !otherManagerIds.contains(wz.getWorker().getWorkerId())) // 다른 공간의 담당자인 작업자는 후보 목록에서 제외
                        .map(WorkerZone::getWorker) // 후보 목록에 포함된 작업자 객체 생성
                        .collect(Collectors.toList()))
                .orElse(zoneWorkers.stream() // 담당자가 없는 경우, 해당 공간 접근 권한이 있는 작업자 목록
                        .filter(wz -> !otherManagerIds.contains(wz.getWorker().getWorkerId())) // 다른 공간의 담당자인 작업자는 후보 목록에서 제외
                        .map(WorkerZone::getWorker) // 후보 목록에 포함된 작업자 객체 생성
                        .collect(Collectors.toList()));

        // 4. DTO 변환 (후보 목록의 작업자들은 현재 이 공간의 담당자가 아니므로 isManager = false)
        return candidates.stream()
                .map(worker -> WorkerManagerResponse.fromEntity(worker, false))
                .collect(Collectors.toList());
    }

    /**
     * 특정 공간의 담당자 지정
     */
    @Transactional
    public void assignManager(String zoneId, String workerId) {
        log.info("공간 ID: {}의 담당자를 작업자 ID: {}로 지정", zoneId, workerId);

        // 1. 작업자-공간 관계 확인
        WorkerZoneId workerZoneId = new WorkerZoneId(workerId, zoneId);
        WorkerZone workerZone = workerZoneRepoService.findById(workerZoneId)
                .orElseThrow(() -> new BadRequestException(
                        String.format("작업자 ID: %s는 공간 ID: %s에 대한 접근 권한이 없습니다.", workerId, zoneId)));

        // 2. 기존 담당자가 있다면 담당자 해제
        Optional<WorkerZone> currentManager = workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId);
        currentManager.ifPresent(manager -> {
            manager.setManageYn(false);
            workerZoneRepoService.save(manager);
        });

        // 3. 새로운 담당자 지정
        workerZone.setManageYn(true);
        workerZoneRepoService.save(workerZone);
    }

    /**
     * 특정 공간의 현재 담당자 조회
     */
    @Transactional(readOnly = true)
    public WorkerManagerResponse getCurrentManager(String zoneId) {
        log.info("공간 ID: {}의 현재 담당자 조회", zoneId);

        return workerZoneRepoService.findByZoneZoneIdAndManageYnIsTrue(zoneId)
                .map(workerZone -> WorkerManagerResponse.fromEntity(workerZone.getWorker(), true))
                .orElse(null);
    }
}
