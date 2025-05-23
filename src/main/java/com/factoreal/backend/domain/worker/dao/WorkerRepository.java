package com.factoreal.backend.domain.worker.dao;

import com.factoreal.backend.domain.worker.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerRepository extends JpaRepository<Worker, String> {
}
