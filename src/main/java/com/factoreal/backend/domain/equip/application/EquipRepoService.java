package com.factoreal.backend.domain.equip.application;

import com.factoreal.backend.domain.equip.dao.EquipRepository;
import com.factoreal.backend.domain.equip.dto.response.EquipInfoResponse;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EquipRepoService {
    private final ZoneRepoService zoneRepoService;
    private final EquipRepository equipRepo;

    /**
     * 설비 정보 저장 레포지토리 접근 메서드
     */
    @Transactional
    protected Equip save(Equip equip) {
        return equipRepo.save(equip);
    }


    public List<Equip> findEquipsByZone(Zone zone) {
        return equipRepo.findEquipsByZone(zone);
    }

    public String findEquipNameByEquipId(String equipId) {
        return equipRepo.findEquipNameByEquipId(equipId);
    }

    /**
     * Id로 설비를 찾는 장비 레포지토리 접근 메서드
     */
    public Equip findById(String equipId) {
        return equipRepo.findById(equipId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 설비 ID: " + equipId));
    }

    public List<EquipInfoResponse> findAll(){
        return equipRepo.findAll().stream()
                .map(equip -> {
                    Zone zone = zoneRepoService.findById(equip.getZone().getZoneId());
                    return new EquipInfoResponse(
                            equip.getEquipId(),
                            equip.getEquipName(),
                            zone.getZoneName(),
                            equip.getZone().getZoneId()
                    );
                })
                .collect(Collectors.toList());
    }
}
