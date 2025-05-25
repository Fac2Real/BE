package com.factoreal.backend.domain.controlLog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.factoreal.backend.domain.controlLog.entity.ControlLog;

/**
 * 자동제어 로그 저장을 위한 레포지토리
 */
public interface ControlLogRepository extends JpaRepository<ControlLog, Long> {
}