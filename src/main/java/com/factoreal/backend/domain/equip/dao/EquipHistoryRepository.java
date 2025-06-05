package com.factoreal.backend.domain.equip.dao;

import com.factoreal.backend.domain.equip.entity.EquipHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquipHistoryRepository extends JpaRepository<EquipHistory, Long> {
    // 설비의 점검 완료되지 않은(checkDate가 null인) 최신 이력 조회
    Optional<EquipHistory> findFirstByEquip_EquipIdAndCheckDateIsNullOrderByIdDesc(String equipId);
}
