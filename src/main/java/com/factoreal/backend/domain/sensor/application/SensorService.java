package com.factoreal.backend.domain.sensor.application;

import com.factoreal.backend.domain.equip.application.EquipRepoService;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.sensor.dto.request.SensorCreateRequest;
import com.factoreal.backend.domain.sensor.dto.request.SensorUpdateRequest;
import com.factoreal.backend.domain.sensor.dto.response.SensorInfoResponse;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorService {
    private final SensorRepoService sensorRepoService;
    private final ZoneRepoService zoneRepoService;
    private final EquipRepoService equipRepoService;

    /**
     * 센서 정보 저장, 하드코딩으로 센서를 저장하고자 할때 사용
     */
    @Transactional
    public Sensor saveSensor(SensorCreateRequest dto) {
        Zone zone = getZoneById(dto.getZoneId());
        Equip equip = getEquipById(dto.getEquipId());

        Sensor sens = new Sensor();
        sens.setSensorId(dto.getSensorId());
        sens.setSensorType(SensorType.valueOf(dto.getSensorType()));
        sens.setZone(zone);
        sens.setEquip(equip);
        sens.setIsZone(dto.getIsZone());
        return sensorRepoService.save(sens);
    }

    /**
     * 모든 센서 리스트 조회 서비스
     */
    public List<SensorInfoResponse> getAllSensors() {
        return sensorRepoService.findAll();
    }

    /**
     * 센서 정보 수정
     */
    @Transactional
    public void updateSensor(String sensorId, SensorUpdateRequest dto) {
        Sensor sensor = sensorRepoService.getSensorById(sensorId);
        sensor.setSensorThres(dto.getSensorThres());
        sensor.setAllowVal(dto.getAllowVal());
        sensorRepoService.save(sensor);
    }

    // 설비 ID를 기반으로 해당 설비에 달린 센서 정보를 조회하는 메서드
    public List<SensorInfoResponse> findSensorsByEquipId(String equipId) {
        return sensorRepoService.findAll().stream()
            .filter(sensor -> sensor.getEquipId().equals(equipId) 
                && !sensor.getEquipId().equals(sensor.getZoneId()))
            .collect(Collectors.toList());
    }

//    /**
//     * 센서 정보 저장을 위한 레포 접근 메서드
//     */
//    @Transactional
//    protected Sensor save(Sensor sensor) {
//        return sensorRepository.save(sensor);
//    }
    
    /**
     * 공간 Id로 공간 조회하는 메서드
     */
    private Zone getZoneById(String zoneId) {
        return zoneRepoService.findById(zoneId);
    }

    /**
     * 설비 Id로 설비를 조회하는 메서드
     */
    private Equip getEquipById(String eqiuipId) {
        return equipRepoService.findById(eqiuipId);
    }

//    /**
//     * 센서 Id로 센서를 조회하는 레포 접근 메서드
//     */
//    public Sensor getSensorById(String sensorId) {
//        return sensorRepository.findById(sensorId)
//                .orElseThrow(() -> new ResponseStatusException(
//                        HttpStatus.NOT_FOUND, "존재하지 않는 센서 ID: " + sensorId));
//    }

//    /**
//     * 공간에 존재하는 센서 리스트를 조회하는 레포 접근 메서드
//     */
//    public List<Sensor> findByZone(Zone zone) {
//        return sensorRepository.findByZone(zone);
//    }
//
//    public Optional<Sensor> findById(String sensorId) {
//        return sensorRepository.findById(sensorId);
//    }
//
//    public List<Sensor> findByZone_ZoneId(String zoneId) {
//        return sensorRepository.findByZone_ZoneId(zoneId);
//    }
}