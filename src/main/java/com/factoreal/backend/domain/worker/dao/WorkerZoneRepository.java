package com.factoreal.backend.domain.worker.dao;

import com.factoreal.backend.domain.worker.entity.WorkerZone;
import com.factoreal.backend.domain.worker.entity.WorkerZoneId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkerZoneRepository extends JpaRepository<WorkerZone, WorkerZoneId> {
    
    // 특정 zone_id에 속한 작업자 목록 조회
    List<WorkerZone> findByZoneZoneId(String zoneId);
    
    // 특정 zone_id의 담당자 조회 (manageYn = true)
    Optional<WorkerZone> findByZoneZoneIdAndManageYnIsTrue(String zoneId);
} 