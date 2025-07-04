package com.factoreal.backend.domain.equip.application;

import com.factoreal.backend.domain.equip.dto.request.EquipCreateRequest;
import com.factoreal.backend.domain.equip.dto.request.EquipUpdateRequest;
import com.factoreal.backend.domain.equip.dto.request.EquipCheckDateRequest;
import com.factoreal.backend.domain.equip.dto.response.EquipInfoResponse;
import com.factoreal.backend.domain.equip.dto.response.EquipWithSensorsResponse;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.equip.entity.EquipHistory;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.sensor.application.SensorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.factoreal.backend.global.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EquipService {
    private final ZoneRepoService zoneRepoService;
    private final EquipRepoService equipRepoService;
    private final EquipHistoryRepoService equipHistoryRepoService;
    private final SensorService sensorService;
    private static final Logger log = LoggerFactory.getLogger(EquipService.class);
    
    /**
     * 설비 생성 서비스
     */
    @Transactional
    public EquipInfoResponse createEquip(EquipCreateRequest req) {
        // 1. UI에서 입력받은 zoneName으로 zoneId 조회
        Zone zone = findByZoneName(req.getZoneName());

        // 2. 고유한 설비ID 생성
        String equipId = IdGenerator.generateId();

        // 3. 설비 정보 저장
        equipRepoService.save(new Equip(equipId, req.getEquipName(), zone));

        return EquipInfoResponse.fromEntity(
            new Equip(equipId, req.getEquipName(), zone),
            zone
        );
    }

    /**
     * 설비명 업데이트 서비스
     */
    @Transactional
    public EquipInfoResponse updateEquip(String equipId, EquipUpdateRequest dto) {
        // 1. 수정할 설비가 존재하는지 확인
        Equip equip = equipRepoService.findById(equipId);

        // 2. 설비명 업데이트
        equip.setEquipName(dto.getEquipName());
        Equip updated = equipRepoService.save(equip);

        Zone zone = findByZoneId(updated.getZone().getZoneId());

        return EquipInfoResponse.fromEntity(updated, zone);
    }

    /**
     * 설비 실제 점검일자 업데이트 서비스
     */
    @Transactional
    public EquipInfoResponse updateCheckDateEquip(String equipId, EquipCheckDateRequest dto) {
        // 1. 수정할 설비가 존재하는지 확인
        Equip equip = equipRepoService.findById(equipId);

        // 2. 점검일자가 있다면 이력 업데이트
        if (dto.getCheckDate() != null) {
            // 2-1. 미점검된 가장 최근 이력 조회
            Optional<EquipHistory> latestUncheckedHistory = equipHistoryRepoService.findLatestUncheckedByEquipId(equipId);
            
            if (latestUncheckedHistory.isPresent()) {
                // 2-2. 점검일자 업데이트
                EquipHistory history = latestUncheckedHistory.get();
                history.setCheckDate(dto.getCheckDate());
                equipHistoryRepoService.save(history);
                log.info("설비 [{}] 실제 점검일자 {}를 저장했습니다.", equip.getEquipName(), dto.getCheckDate());
            } else {
                log.warn("설비 [{}] 미점검된 예상 점검일자가 없습니다.", equip.getEquipName());
            }
        }

        Zone zone = findByZoneId(equip.getZone().getZoneId());
        return EquipInfoResponse.fromEntity(equip, zone);
    }

    /**
     * 모든 설비 리스트 조회
     */
    public List<EquipInfoResponse> getAllEquips() {
        return equipRepoService.findAll();
    }

    // 설비 정보와 센서 정보를 함께 반환하는 메서드 (BE -> FE)
    public List<EquipWithSensorsResponse> getEquipsByZoneId(String zoneId) {
        Zone zone = findByZoneId(zoneId);
        return equipRepoService.findEquipsByZone(zone).stream()
            .map(equip -> {
                // 최근 실제 점검일자 조회 (checkDate가 null이 아닌 것 중에서 가장 최근 날짜)
                LocalDate lastCheckDate = equipHistoryRepoService
                    .findLatestCheckedByEquipId(equip.getEquipId())
                    .map(EquipHistory::getCheckDate)
                    .orElse(null);

                return EquipWithSensorsResponse.fromEntity(
                    equip,
                    zone,
                    lastCheckDate,
                    sensorService.findSensorsByEquipId(equip.getEquipId())
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * 이름으로 공간 탐색
     */
    private Zone findByZoneName(String zoneName) {
        return zoneRepoService.findByZoneName(zoneName);
    }

    /**
     * 공간 ID로 공간 탐색
     */
    private Zone findByZoneId(String zoneId) {
        return zoneRepoService.findById(zoneId);
    }

//    /**
//     * Id로 설비를 찾는 장비 레포지토리 접근 메서드
//     */
//    public Equip findById(String equipId) {
//        return equipRepo.findById(equipId)
//                .orElseThrow(() -> new ResponseStatusException(
//                        HttpStatus.NOT_FOUND, "존재하지 않는 설비 ID: " + equipId));
//    }

//    /**
//     * 설비 정보 저장 레포지토리 접근 메서드
//     */
//    @Transactional
//    protected Equip save(Equip equip) {
//        return equipRepo.save(equip);
//    }

//    public List<Equip> findEquipsByZone(Zone zone) {
//        return equipRepo.findEquipsByZone(zone);
//    }
//
//    public String findEquipNameByEquipId(String equipId) {
//        return equipRepo.findEquipNameByEquipId(equipId);
//    }
}
