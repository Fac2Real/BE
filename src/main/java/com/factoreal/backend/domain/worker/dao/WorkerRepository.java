package com.factoreal.backend.domain.worker.dao;

import com.factoreal.backend.domain.worker.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WorkerRepository extends JpaRepository<Worker, String> {
    List<Worker> findByWorkerIdIn(Collection<String> ids);
}
