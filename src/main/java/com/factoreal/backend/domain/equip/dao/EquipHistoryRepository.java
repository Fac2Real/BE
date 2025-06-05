package com.factoreal.backend.domain.equip.dao;

import com.factoreal.backend.domain.equip.entity.EquipHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquipHistoryRepository extends JpaRepository<EquipHistory, Long> {
    // 설비의 점검 완료되지 않은(checkDate가 null인) 최신 이력 조회
    Optional<EquipHistory> findFirstByEquip_EquipIdAndCheckDateIsNullOrderByIdDesc(String equipId);
    
    // 설비의 가장 최근 실제 점검일자 조회 (checkDate가 null이 아닌 것 중에서)
    Optional<EquipHistory> findFirstByEquip_EquipIdAndCheckDateIsNotNullOrderByCheckDateDesc(String equipId);
}
