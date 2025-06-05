package com.factoreal.backend.domain.abnormalLog.dao;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.zone.entity.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    /** 일정 기간의 Abn 로그를 조회하는 기능 */
    List<AbnormalLog> findByDetectedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT a
        FROM AbnormalLog a
        WHERE a.targetType = :targetType
          AND a.targetId IN (
              SELECT s.sensorId
              FROM Sensor s
              WHERE s.zone = :zone
          )
          AND a.dangerLevel = :dangerLevel
        ORDER BY a.detectedAt DESC
        LIMIT 1
    """)
    Optional<AbnormalLog> findLatestSensorLogInZoneWithDangerLevel(
        @Param("targetType") TargetType targetType,
        @Param("zone") Zone zone,
        @Param("dangerLevel") Integer dangerLevel
    );

    /**
     * 30일 기간의 이상치(위험도 1과 2) 에 해당되는 ABN 로그를 불러오는 기능
     */
    List<AbnormalLog> findByDetectedAtBetweenAndDangerLevelIn(
            LocalDateTime start, LocalDateTime end, List<Integer> dangerLevels);
}