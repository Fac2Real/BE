package com.factoreal.backend.domain.equip.application;

import com.factoreal.backend.domain.equip.dao.EquipHistoryRepository;
import com.factoreal.backend.domain.equip.entity.EquipHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 설비의 점검 완료되지 않은(checkDate가 null인) 최신 이력 조회
    @Transactional(readOnly = true)
    public Optional<EquipHistory> findLatestUncheckedByEquipId(String equipId) {
        return equipHistoryRepository.findFirstByEquip_EquipIdAndCheckDateIsNullOrderByIdDesc(equipId);
    }

    // 설비의 가장 최근 실제 점검일자 조회
    @Transactional(readOnly = true)
    public Optional<EquipHistory> findLatestCheckedByEquipId(String equipId) {
        return equipHistoryRepository.findFirstByEquip_EquipIdAndCheckDateIsNotNullOrderByCheckDateDesc(equipId);
    }
} 