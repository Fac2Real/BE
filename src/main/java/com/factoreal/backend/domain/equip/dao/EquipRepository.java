package com.factoreal.backend.domain.equip.dao;

import java.util.List;
import java.util.Optional;

import com.factoreal.backend.domain.zone.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import com.factoreal.backend.domain.equip.entity.Equip;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 설비 엔티티(JPA)의 Repository
 */
public interface EquipRepository extends JpaRepository<Equip, String> {
    Optional<Equip> findByEquipId(String equipId);

    @Query("select e.equipName from Equip e where e.equipId = :equipId")
    String findEquipNameByEquipId(@Param("equipId") String equipId);

//    @Query("select e from Equip e where e.zone = :zone and e.equipId <> :zoneId")
//    List<Equip> findFacilitiesByZone(@Param("zone") Zone zone);

    List<Equip> findEquipsByZone(@Param("zone") Zone zone);

    // equipId와 zone.zoneId가 다른 설비만 조회하는 JPA 쿼리
    @Query("SELECT e FROM Equip e WHERE e.equipId <> e.zone.zoneId")
    List<Equip> findAllWhereEquipIdNotEqualsZoneId();
}