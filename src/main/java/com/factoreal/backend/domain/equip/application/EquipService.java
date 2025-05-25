package com.factoreal.backend.domain.equip.application;

import com.factoreal.backend.domain.equip.dto.request.EquipCreateRequest;
import com.factoreal.backend.domain.equip.dto.request.EquipUpdateRequest;
import com.factoreal.backend.domain.equip.dto.response.EquipInfoResponse;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.equip.dao.EquipRepository;
import com.factoreal.backend.domain.zone.dao.ZoneRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.factoreal.backend.global.util.IdGenerator;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipService {
    private final EquipRepository equipRepo;
    private final ZoneRepository zoneRepo;

    /** 설비 생성 서비스 */
    @Transactional
    public EquipInfoResponse createEquip(EquipCreateRequest req) {
        // 1. UI에서 입력받은 zoneName으로 zoneId 조회
        Zone zone = findByZoneName(req.getZoneName());

        // 2. 고유한 설비ID 생성
        String equipId = IdGenerator.generateId();

        // 3. 설비 정보 저장
        save(new Equip(equipId, req.getEquipName(), zone));

        return new EquipInfoResponse(equipId, req.getEquipName(), zone.getZoneName(), zone.getZoneId());
    }

    /** 설비 정보 수정 서비스 */
    @Transactional
    public EquipInfoResponse updateEquip(String equipId, EquipUpdateRequest dto) {
        // 1. 수정할 설비가 존재하는지 확인
        Equip equip = findById(equipId);

        // 2. 설비명 업데이트
        equip.setEquipName(dto.getEquipName());
        Equip updated = save(equip);

        Zone zone = findByZoneId(updated.getZone().getZoneId());

        return new EquipInfoResponse(
            updated.getEquipId(),
            updated.getEquipName(),
            zone.getZoneName(),
            zone.getZoneId()
        );

    }

    /** 모든 설비리스트 조회 */
    public List<EquipInfoResponse> getAllEquips() {
        return equipRepo.findAll().stream()
        .map(equip -> {
            Zone zone = findByZoneId(equip.getZone().getZoneId());
            return new EquipInfoResponse(
                equip.getEquipId(),
                equip.getEquipName(),
                zone.getZoneName(),
                equip.getZone().getZoneId()
            );
        })
        .collect(Collectors.toList());
    }

    /** 이름으로 공간 탐색 */
    private Zone findByZoneName(String zoneName) {
        return zoneRepo.findByZoneName(zoneName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "존재하지 않는 공간명: " + zoneName));
    }

    /** 공간 ID로 공간 탐색 */
    private Zone findByZoneId(String zoneId) {
        return zoneRepo.findById(zoneId)
                .orElse(new Zone("", "미등록 공간"));
    }

    /** Id로 설비를 찾는 장비 레포지토리 접근 메서드 */
    private Equip findById (String equipId){
        return equipRepo.findById(equipId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 설비 ID: " + equipId));
    }

    /** 설비 정보 저장 레포지토리 접근 메서드 */
    @Transactional
    protected Equip save(Equip equip) {
        return equipRepo.save(equip);
    }

}
