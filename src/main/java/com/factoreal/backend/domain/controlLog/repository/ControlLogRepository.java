package com.factoreal.backend.domain.controlLog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.factoreal.backend.domain.controlLog.entity.ControlLog;

import java.util.Collection;
import java.util.List;

/**
 * 자동제어 로그 저장을 위한 레포지토리
 */
public interface ControlLogRepository extends JpaRepository<ControlLog, Long> {
    /** AbnormalLog ID 목록으로 대응(제어) 로그 조회 */
    List<ControlLog> findByAbnormalLog_IdIn(Collection<Long> abnormalIds);
}