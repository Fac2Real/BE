package com.factoreal.backend.domain.equip.dao;

import com.factoreal.backend.domain.equip.entity.EquipHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipHistoryRepository extends JpaRepository<EquipHistory, Long> {
}
