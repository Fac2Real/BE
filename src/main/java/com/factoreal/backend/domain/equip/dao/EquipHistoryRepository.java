package com.factoreal.backend.domain.equip.dao;

import com.factoreal.backend.domain.equip.entity.EquipHistory;
import com.factoreal.backend.domain.equip.entity.EquipHistoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface EquipHistoryRepository extends JpaRepository<EquipHistory, Long> {
    // 설비 최신 기록 조회
    Optional<EquipHistory> findFirstByEquip_EquipIdAndTypeOrderByDateDesc(String equipId, EquipHistoryType type);

    // 특정 날짜의 이력 존재 여부 확인
    Optional<EquipHistory> findFirstByEquip_EquipIdAndTypeAndDate(String equipId, EquipHistoryType type, LocalDate date);
}
