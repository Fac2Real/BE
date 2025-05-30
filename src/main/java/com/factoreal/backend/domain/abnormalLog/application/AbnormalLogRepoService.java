package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dao.AbnLogRepository;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

    public List<AbnormalLog> findPreviewMonthLog(){
        // 오늘을 기준으로 전달 결정 -> 오늘이 2025-05-?? 일 경우
        LocalDateTime startOfThisMonth = LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay();           // 2025-05-01 00:00:00 이 로그의 종점이 됨 (이번 달)
        LocalDateTime startOfPrevMonth = startOfThisMonth.minusMonths(1);    // 2025-04-01 00:00:00이 시작점이 됨 (이전 달)

        // 예시 기준 4/1일부터 5월 1일까지의 로그가 리스트로 반환됨
        return abnLogRepository.findByDetectedAtBetween(startOfPrevMonth, startOfThisMonth)
                .stream()
                .filter(l -> l.getDangerLevel() == 1 || l.getDangerLevel() == 2)
                .toList();
    }

    public AbnormalLog save(AbnormalLog abnormalLog) {
        return  abnLogRepository.save(abnormalLog);
    }

    public Page<AbnormalLog> findAll(Pageable pageable) {
        return abnLogRepository.findAll(pageable);
    }

    public Optional<AbnormalLog> findById(Long id) {
        return abnLogRepository.findById(id);
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
}
