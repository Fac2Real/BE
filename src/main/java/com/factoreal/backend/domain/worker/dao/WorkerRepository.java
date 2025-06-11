package com.factoreal.backend.domain.worker.dao;

import com.factoreal.backend.domain.worker.entity.Worker;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WorkerRepository extends JpaRepository<Worker, String> {
    List<Worker> findByWorkerIdIn(Collection<String> ids);

    /**
     * 중복 작업자 정보 체크용 메서드
     */
    boolean existsByWorkerId(String workerId);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    // 예시 코드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Worker findWorkersByWorkerId(@Param("workerId") String workerId);
}
