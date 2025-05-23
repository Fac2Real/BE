package com.factoreal.backend.domain.abnormalLog.dao;

import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbnLogRepository extends JpaRepository<AbnormalLog,Long> {
    Page<AbnormalLog> findAbnormalLogsByAbnormalType(String abnormalType, Pageable pageable);

    Page<AbnormalLog> findAbnormalLogsByTargetTypeAndTargetId(String targetType, String targetId, Pageable pageable);
    long countByIsReadFalse(); // 읽지 않은 로그의 개수 반환

    Page<AbnormalLog> findAllByIsReadIsFalse(Pageable pageable);

    // zoneId로 페이징 처리된 로그 조회
    Page<AbnormalLog> findByZone_ZoneIdOrderByDetectedAtDesc(String zoneId, Pageable pageable);
}