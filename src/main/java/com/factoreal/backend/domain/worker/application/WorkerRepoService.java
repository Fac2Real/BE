package com.factoreal.backend.domain.worker.application;

import com.factoreal.backend.domain.worker.dao.WorkerRepository;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkerRepoService {
    private final WorkerRepository workerRepository;

    public Worker findById(String workerId) {
        return workerRepository.findById(workerId)
                .orElseThrow(() -> new NotFoundException("작업자를 찾을 수 없습니다: " + workerId));
    }

    @Transactional
    public Worker save(Worker worker) {
        return workerRepository.save(worker);
    }
    /**
     * workerId에 해당하는 작업자 조회
     */
    @Transactional(readOnly = true)
    public Worker getWorkerByWorkerId(String workerId) {
        return workerRepository.findById(workerId).orElseThrow();
    }

    /**
     * 모든 작업자 리스트를 조회하는 레포 접근 메서드
     */
    public List<Worker> findAll() {
        return workerRepository.findAll();
    }


}
