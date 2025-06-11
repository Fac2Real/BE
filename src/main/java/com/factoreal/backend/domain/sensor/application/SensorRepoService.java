package com.factoreal.backend.domain.sensor.application;

import com.factoreal.backend.domain.sensor.dao.SensorRepository;
import com.factoreal.backend.domain.sensor.dto.response.SensorInfoResponse;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorRepoService {

    private final SensorRepository sensorRepository;

    /**
     * 공간에 존재하는 센서 리스트를 조회하는 레포 접근 메서드
     */
    public List<Sensor> findByZone(Zone zone) {
        return sensorRepository.findByZone(zone);
    }

    /**
     * 모든 센서 리스트 조회하는 메서드
     */
    public List<SensorInfoResponse> findAll(){
        return sensorRepository.findAll().stream()
                .map(s -> new SensorInfoResponse(
                        s.getSensorId(),
                        s.getSensorType().toString(),
                        s.getSensorType().name(),
                        s.getZone().getZoneId(),
                        s.getEquip().getEquipId(),
                        s.getSensorThres(),
                        s.getAllowVal(),
                        s.getIsZone()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 센서 정보 저장을 위한 레포 접근 메서드
     */
    @Transactional
    protected Sensor save(Sensor sensor) {
        return sensorRepository.save(sensor);
    }

    public Sensor findById(String sensorId) {
        return sensorRepository.findById(sensorId)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 센서 ID: "+ sensorId));
    }

    public List<Sensor> findByZone_ZoneId(String zoneId) {
        return sensorRepository.findByZone_ZoneId(zoneId);
    }

    /**
     * 센서 Id로 센서를 조회하는 레포 접근 메서드
     */
    public Sensor getSensorById(String sensorId) {
        return sensorRepository.findById(sensorId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 센서 ID: " + sensorId));
    }


    /**
     * Abnormal에서의 target_id(센서_id)로 매칭되는 설비별 센서들을 탐색하는 메서드
     */
    public Map<String,String> sensorIdToEquipId(List<String> ids) {
        return sensorRepository.findBySensorIdIn(ids).stream()
                .collect(Collectors.toMap(
                        Sensor::getSensorId,
                        s -> s.getEquip().getEquipId()
                ));
    }
}
