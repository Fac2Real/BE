package com.factoreal.backend.domain.abnormalLog.dao;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AbnLogRepository extends JpaRepository<AbnormalLog,Long> {
    Page<AbnormalLog> findAbnormalLogsByAbnormalType(String abnormalType, Pageable pageable);

    Page<AbnormalLog> findAbnormalLogsByTargetTypeAndTargetId(TargetType targetType, String targetId, Pageable pageable);
    // Pageable 객체 없이도 사용할 수 있도록 오버라이딩
    Optional<AbnormalLog> findFirstByTargetTypeAndTargetIdOrderByDetectedAtDesc(TargetType targetType, String targetId);


    long countByIsReadFalse(); // 읽지 않은 로그의 개수 반환

    Page<AbnormalLog> findAllByIsReadIsFalseOrderByDetectedAtDesc(Pageable pageable);

    // zoneId로 페이징 처리된 로그 조회
    Page<AbnormalLog> findByZone_ZoneIdOrderByDetectedAtDesc(String zoneId, Pageable pageable);
}