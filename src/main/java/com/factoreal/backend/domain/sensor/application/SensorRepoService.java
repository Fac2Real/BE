package com.factoreal.backend.domain.sensor.application;

import com.factoreal.backend.domain.sensor.dao.SensorRepository;
import com.factoreal.backend.domain.sensor.dto.response.SensorInfoResponse;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.zone.entity.Zone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
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

    public Optional<Sensor> findById(String sensorId) {
        return sensorRepository.findById(sensorId);
    }

    public List<Sensor> findByZone_ZoneId(String zoneId) {
        return sensorRepository.findByZone_ZoneId(zoneId);
    }

    /**
     * 센서 Id로 센서를 조회하는 레포 접근 메서드
     */
    public Sensor getSensorById(String sensorId) {
        return sensorRepository.findById(sensorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 센서 ID: " + sensorId));
    }

}
