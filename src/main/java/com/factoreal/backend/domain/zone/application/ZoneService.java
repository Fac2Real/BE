package com.factoreal.backend.domain.zone.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.factoreal.backend.domain.abnormalLog.dao.AbnLogRepository;
import com.factoreal.backend.domain.abnormalLog.dto.request.AbnormalPagingRequest;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.equip.dto.response.EquipDetailResponse;
import com.factoreal.backend.domain.sensor.application.SensorService;
import com.factoreal.backend.domain.sensor.dto.response.SensorInfoResponse;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.equip.dao.EquipRepository;
import com.factoreal.backend.domain.sensor.dao.SensorRepository;
import com.factoreal.backend.domain.zone.dto.request.ZoneCreateRequest;
import com.factoreal.backend.domain.zone.dto.request.ZoneUpdateRequest;
import com.factoreal.backend.domain.zone.dto.response.ZoneDetailResponse;
import com.factoreal.backend.domain.zone.dto.response.ZoneInfoResponse;
import com.factoreal.backend.domain.zone.dto.response.ZoneLogResponse;
import com.factoreal.backend.global.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.dao.ZoneRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class ZoneService {
    private final ZoneRepository zoneRepository;
    private final SensorRepository sensorRepository;
    private final EquipRepository equipRepository;
    private final AbnLogRepository abnLogRepository;

    private final SensorService sensorService;

    @Transactional
    public ZoneInfoResponse createZone(ZoneCreateRequest zoneCreateRequest) {
        String zoneName = zoneCreateRequest.getZoneName();
        String zoneId = IdGenerator.generateId();

        validateZoneName(zoneName);

        Zone zone = zoneRepository.save(new Zone(zoneId, zoneName));
        return ZoneInfoResponse.from(zone);
    }

    @Transactional
    public ZoneInfoResponse updateZone(String zoneName, ZoneUpdateRequest dto) {
        // 1. 수정할 공간이 존재하는지 확인
        Zone zone = getZoneByName(zoneName);

        // 2. 새로운 공간명이 이미 존재하는지 확인
        if (!zone.getZoneName().equals(dto.getZoneName())) {
            validateZoneName(dto.getZoneName());
        }

        zone.setZoneName(dto.getZoneName());
        Zone updatedZone = zoneRepository.save(zone);
        return ZoneInfoResponse.from(updatedZone);
    }

    public List<ZoneInfoResponse> getAllZones() {
        return zoneRepository.findAll().stream()
                .map(ZoneInfoResponse::from)
                .collect(Collectors.toList());
    }


    /**
     *  Zone별 환경‧설비 센서를 모아 ZoneDetailResponse DTO로 반환한다.
     *
     *  <pre>
     *  ① Zone 조회
     *  ② 각 Zone에 대한 센서·설비 매핑
     *  ③ env, fac 센서 DTO 변환 및 그룹핑
     *  </pre>
     */
    public List<ZoneDetailResponse> getZoneItems() {
        // ① 전체 Zone 조회
        return findAll().stream()          // Repository 직접 접근 대신 Reader 사용 예
                .map(this::buildZoneDetailResponse)        // ② 변환 (깊이 1 유지)
                .toList();
    }
    /** ② 각 Zone에 대한 센서·설비 매핑 */
    private ZoneDetailResponse buildZoneDetailResponse(Zone zone) {
        List<SensorInfoResponse> envDtos  = getEnvSensorDtos(zone);       // ③-1
        List<EquipDetailResponse> facDtos = getEquipDetailDtos(zone);     // ③-2

        return ZoneDetailResponse.builder()
                .zoneName(zone.getZoneName())
                .envList(envDtos)
                .equipList(facDtos)
                .build();
    }
    /** ③-1 환경 센서 DTO 목록 생성 */
    private List<SensorInfoResponse> getEnvSensorDtos(Zone zone) {
        List<Sensor> envSensors = sensorService.findByZone(zone)
                .stream().filter(s -> Objects.equals(s.getZone().getZoneId(), s.getEquip().getEquipId()))
                .toList();
        return envSensors.stream().map(SensorInfoResponse::from).toList();
    }
    /** ③-2 설비 + 설비센서 DTO 목록 생성 */
    private List<EquipDetailResponse> getEquipDetailDtos(Zone zone) {
        // 1) 설비 조회
        List<Equip> equips = findEquipsByZone(zone).stream()
                .filter(e -> e.getEquipName() != null && !e.getEquipName().equalsIgnoreCase("empty"))
                .toList();   // empty이름을 가진 설비(환경센서)는 설비 목록에서 제외하기

        // 2) 설비센서 그룹핑
        Map<String, List<SensorInfoResponse>> facGroup = sensorService.findByZone(zone)
                .stream()
                .filter(s -> !Objects.equals(s.getZone().getZoneId(), s.getEquip().getEquipId()))
                .map(SensorInfoResponse::from)                 // ★ Sensor → SensorDto
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

    @Transactional
    public Page<ZoneLogResponse> findSystemLogsByZoneId(String zoneId, AbnormalPagingRequest pagingDto) {
        log.info("공간 ID: {}의 시스템 로그 조회", zoneId);
        Pageable pageable = getPageable(pagingDto);

        Page<AbnormalLog> logs = abnLogRepository.findByZone_ZoneIdOrderByDetectedAtDesc(zoneId, pageable);
        return logs.map(ZoneLogResponse::from);
    }

    /** getZoneItems 서비스를 위한 레포지토리 접근 서비스 1*/
    private List<Zone> findAll() {
        return zoneRepository.findAll();
    }
    /** getZoneItems 서비스를 위한 레포지토리 접근 서비스 2*/
    private List<Equip> findEquipsByZone(Zone zone) {
        return equipRepository.findEquipsByZone(zone).stream()
                .filter(e -> e.getEquipName() != null &&
                        !"empty".equalsIgnoreCase(e.getEquipName()))
                .toList();
    }
    /** getZoneItems 서비스를 위한 레포지토리 접근 서비스 3*/
    private String findEquipNameByEquipId(String equipId) {
        return equipRepository.findEquipNameByEquipId(equipId);
    }

    private Zone getZoneByName(String zoneName) {
        return zoneRepository.findByZoneName(zoneName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 공간: " + zoneName));
    }

    private void validateZoneName(String zoneName) {
        if (zoneRepository.findByZoneName(zoneName).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "이미 존재하는 공간명: " + zoneName);
        }
    }

    private Pageable getPageable(AbnormalPagingRequest abnormalPagingDto){
        return PageRequest.of(
                abnormalPagingDto.getPage(),
                abnormalPagingDto.getSize()
        );
    }
}