package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dao.AbnLogRepository;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.exception.dto.BadRequestException;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AbnormalLogRepoService{

    private final AbnLogRepository abnLogRepository;

    public Page<AbnormalLog> findByZone_ZoneIdOrderByDetectedAtDesc(String zoneId, Pageable pageable) {
        return abnLogRepository.findByZone_ZoneIdOrderByDetectedAtDesc(zoneId, pageable);
    }

    public List<AbnormalLog> findPreview30daysLog(){
        // ① 오늘 날짜와 30일 전 시각 계산
        LocalDateTime now        = LocalDateTime.now().minusDays(1);          // 현재 시각
        LocalDateTime thirtyDays = now.minusDays(30);            // 30일 전

        // ② DB 조회 + dangerLevel 1,2 필터
        return abnLogRepository.findByDetectedAtBetween(thirtyDays, now)
                .stream()
                .filter(l -> {
                    Integer dl = l.getDangerLevel();
                    return dl != null && (dl == 1 || dl == 2);
                })
                .toList();
    }

    public AbnormalLog save(AbnormalLog abnormalLog) {
        return  abnLogRepository.save(abnormalLog);
    }

    public Page<AbnormalLog> findAll(Pageable pageable) {
        return abnLogRepository.findAll(pageable);
    }

    public AbnormalLog findById(Long id) {
        return abnLogRepository.findById(id).orElseThrow(() -> new NotFoundException("AbnLog not found"));
    }

    public Page<AbnormalLog> findAllByIsReadIsFalseOrderByDetectedAtDesc(Pageable pageable) {
        return abnLogRepository.findAllByIsReadIsFalseOrderByDetectedAtDesc(pageable);
    }

    public Page<AbnormalLog> findAbnormalLogsByAbnormalType(Pageable pageable, String  abnormalType) {
        return abnLogRepository.findAbnormalLogsByAbnormalType(abnormalType, pageable);
    }

    public Optional<AbnormalLog> findFirstByTargetTypeAndTargetIdOrderByDetectedAtDesc(TargetType targetType, String targetId) {
        return abnLogRepository.findFirstByTargetTypeAndTargetIdOrderByDetectedAtDesc(targetType, targetId);
    }
    public Page<AbnormalLog> findAbnormalLogsByTargetTypeAndTargetId(Pageable pageable, TargetType targetType, String targetId) {
        return abnLogRepository.findAbnormalLogsByTargetTypeAndTargetId(targetType, targetId,pageable);
    }
    public Long countByIsReadFalse(){
        return abnLogRepository.countByIsReadFalse();
    }

    /**
     *  특정 zone에서 마지막으로 발생한 AbnormalLog 조회
     */
    public AbnormalLog findLatestSensorLogInZoneWithDangerLevel(TargetType targetType,
                                                                Zone zone,
                                                                Integer dangerLevel){
        Optional<AbnormalLog> abnormalLog = abnLogRepository.findLatestSensorLogInZoneWithDangerLevel(targetType, zone, dangerLevel);
        abnormalLog.orElseThrow(
            () -> new BadRequestException("조건에 맞는 AbnormalLog가 없습니다.")
        );
        return abnormalLog.get();
    }
}
