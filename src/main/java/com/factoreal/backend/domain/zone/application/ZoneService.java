package com.factoreal.backend.domain.zone.application;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.dto.request.AbnormalPagingRequest;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.equip.application.EquipRepoService;
import com.factoreal.backend.domain.equip.dto.response.EquipDetailResponse;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.dto.response.SensorInfoResponse;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.zone.dto.request.ZoneCreateRequest;
import com.factoreal.backend.domain.zone.dto.request.ZoneUpdateRequest;
import com.factoreal.backend.domain.zone.dto.response.ZoneDetailResponse;
import com.factoreal.backend.domain.zone.dto.response.ZoneInfoResponse;
import com.factoreal.backend.domain.zone.dto.response.ZoneLogResponse;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.exception.dto.DuplicateResourceException;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import com.factoreal.backend.global.util.IdGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ZoneService {

    private final SensorRepoService sensorRepoService;
    private final ZoneRepoService zoneRepoService;
    private final EquipRepoService equipRepoService;
    private final AbnormalLogRepoService abnormalLogRepoService;

    /**
     * 존 생성 서비스
     */
    @Transactional
    public ZoneInfoResponse createZone(ZoneCreateRequest zoneCreateRequest) {
        String zoneName = zoneCreateRequest.getZoneName();
        String zoneId = IdGenerator.generateId();

        zoneRepoService.validateZoneName(zoneName);

        Zone zone = zoneRepoService.save(new Zone(zoneId, zoneName));

        // zoneId 로 equip_info 에 저장해주기
        equipRepoService.save(new Equip(zoneId, "empty", zone));
        return ZoneInfoResponse.fromEntity(zone);
    }

    /**
     * 존 업데이트 서비스
     */
    @Transactional
    public ZoneInfoResponse updateZone(String zoneName, ZoneUpdateRequest dto) {
        // 1. 수정할 공간이 존재하는지 확인
        Zone zone = zoneRepoService.getZoneByName(zoneName);

        // 2. 새로운 공간명이 이미 존재하는지 확인
        if (!zone.getZoneName().equals(dto.getZoneName())) {
            zoneRepoService.validateZoneName(dto.getZoneName());
        }else{
            throw new DuplicateResourceException("중복되는 공간 이름이 존재합니다.");
        }

        zone.setZoneName(dto.getZoneName());
        Zone updatedZone = zoneRepoService.save(zone);
        return ZoneInfoResponse.fromEntity(updatedZone);
    }

    /**
     * 모든 공간을 조회하는 서비스
     */
    public List<ZoneInfoResponse> getAllZones() {
        List<Zone> zones = zoneRepoService.findAll();
        if (zones.isEmpty()) {
            // 공간 없을 시 예외 처리 추가
            throw new NotFoundException("등록된 공간이 없습니다.");
        }
        return zoneRepoService.findAll().stream()
                .map(ZoneInfoResponse::fromEntity)
                .collect(Collectors.toList());
    }


    /**
     * Zone별 환경‧설비 센서를 모아 ZoneDetailResponse DTO로 반환하는 서비스
     *
     * <pre>
     *  ① Zone 조회
     *  ② 각 Zone에 대한 센서·설비 매핑
     *  ③ env, fac 센서 DTO 변환 및 그룹핑
     *  </pre>
     */
    public List<ZoneDetailResponse> getZoneItems() {
        // ① 전체 Zone 조회
        return zoneRepoService.findAll().stream()          // Repository 직접 접근 대신 Reader 사용 예
                .map(this::buildZoneDetailResponse)        // ② 변환 (깊이 1 유지)
                .toList();
    }

    /**
     * ② 각 Zone에 대한 센서·설비 매핑
     */
    private ZoneDetailResponse buildZoneDetailResponse(Zone zone) {
        List<SensorInfoResponse> envDtos = getEnvSensorDtos(zone);       // ③-1
        List<EquipDetailResponse> facDtos = getEquipDetailDtos(zone);     // ③-2

        return ZoneDetailResponse.builder()
                .zoneId(zone.getZoneId())
                .zoneName(zone.getZoneName())
                .envList(envDtos)
                .equipList(facDtos)
                .build();
    }

    /**
     * ③-1 환경 센서 DTO 목록 생성
     */
    private List<SensorInfoResponse> getEnvSensorDtos(Zone zone) {
        List<Sensor> envSensors = sensorRepoService.findByZone(zone)
                .stream().filter(s -> Objects.equals(s.getZone().getZoneId(), s.getEquip().getEquipId()))
                .toList();
        return envSensors.stream().map(SensorInfoResponse::fromEntity).toList();
    }

    /**
     * ③-2 설비 + 설비센서 DTO 목록 생성
     */
    private List<EquipDetailResponse> getEquipDetailDtos(Zone zone) {
        // 1) 설비 조회 (환경센서 empty 인 경우는 제외한다)
        List<Equip> equips = findEquipsByZone(zone).stream()
                .filter(e -> !e.getEquipName().equalsIgnoreCase("empty"))
                .toList();   // empty이름을 가진 설비(환경센서)는 설비 목록에서 제외하기

        // 2) 설비센서 그룹핑
        Map<String, List<SensorInfoResponse>> facGroup = sensorRepoService.findByZone(zone)
                .stream()
                .filter(s -> !Objects.equals(s.getZone().getZoneId(), s.getEquip().getEquipId()))
                .map(SensorInfoResponse::fromEntity)                 // ★ Sensor → SensorDto
                .collect(Collectors.groupingBy(SensorInfoResponse::getEquipId));

        // 3) DTO 변환
        return equips.stream()
                .map(e -> EquipDetailResponse.builder()
                        .equipId(e.getEquipId())
                        .equipName(e.getEquipName())
                        .facSensor(facGroup.getOrDefault(e.getEquipId(), List.of()))
                        .build())
                .toList();
    }

    /**
     * 공간Id로 시스템 로그를 찾는 서비스
     */
    @Transactional
    public Page<ZoneLogResponse> findSystemLogsByZoneId(String zoneId, AbnormalPagingRequest pagingDto) {
        log.info("공간 ID: {}의 시스템 로그 조회", zoneId);
        Pageable pageable = getPageable(pagingDto);

        Page<AbnormalLog> logs = abnormalLogRepoService.findByZone_ZoneIdOrderByDetectedAtDesc(zoneId, pageable);
        return logs.map(ZoneLogResponse::fromEntity);
    }

    /**
     * getZoneItems 서비스를 위한 레포지토리 접근 서비스 2
     */
    private List<Equip> findEquipsByZone(Zone zone) {
        return equipRepoService.findEquipsByZone(zone).stream()
                .filter(e -> e.getEquipName() != null &&
                        !"empty".equalsIgnoreCase(e.getEquipName()))
                .toList();
    }

    private Pageable getPageable(AbnormalPagingRequest abnormalPagingDto) {
        return PageRequest.of(
                abnormalPagingDto.getPage(),
                abnormalPagingDto.getSize()
        );
    }
}