package com.factoreal.backend.domain.worker.dao;

import com.factoreal.backend.domain.worker.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface WorkerRepository extends JpaRepository<Worker, String> {
    List<Worker> findByWorkerIdIn(Collection<String> ids);

    /**
     * 중복 작업자 체크용 메서드
     */
    boolean existsByWorkerId(String workerId);
    boolean existsByPhoneNumber(String phoneNumber);

}
