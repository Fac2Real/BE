package com.factoreal.backend.domain.equip.application;

import com.factoreal.backend.domain.equip.dao.EquipHistoryRepository;
import com.factoreal.backend.domain.equip.entity.EquipHistory;
import com.factoreal.backend.domain.equip.entity.EquipHistoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EquipHistoryRepoService {
    private final EquipHistoryRepository equipHistoryRepository;

    @Transactional
    public EquipHistory save(EquipHistory history) {
        return equipHistoryRepository.save(history);
    }

    // 설비의 최신 기록 조회
    @Transactional(readOnly = true)
    public Optional<EquipHistory> findFirstByEquip_EquipIdAndTypeOrderByDateDesc(String equipId, EquipHistoryType type) {
        return equipHistoryRepository.findFirstByEquip_EquipIdAndTypeOrderByDateDesc(equipId, type);
    }

    // 특정 날짜의 이력 존재 여부 확인
    @Transactional(readOnly = true)
    public Optional<EquipHistory> findFirstByEquip_EquipIdAndTypeAndDate(String equipId, EquipHistoryType type, LocalDate date) {
        return equipHistoryRepository.findFirstByEquip_EquipIdAndTypeAndDate(equipId, type, date);
    }
} 