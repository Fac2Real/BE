package com.factoreal.backend.domain.sensor.application;

import java.util.List;
import java.util.stream.Collectors;

import com.factoreal.backend.domain.sensor.dto.request.SensorCreateRequest;
import com.factoreal.backend.domain.sensor.dto.response.SensorInfoResponse;
import com.factoreal.backend.domain.sensor.dto.request.SensorUpdateRequest;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.equip.dao.EquipRepository;
import com.factoreal.backend.domain.sensor.dao.SensorRepository;
import com.factoreal.backend.domain.zone.dao.ZoneRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorService {
    private final SensorRepository sensorRepository;
    private final ZoneRepository zoneRepository;
    private final EquipRepository equipRepository;

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
        return sensorRepository.save(sens);
    }

    public List<SensorInfoResponse> getAllSensors() {
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

    @Transactional
    public void updateSensor(String sensorId, SensorUpdateRequest dto) {
        Sensor sensor = getSensorById(sensorId);
        sensor.setSensorThres(dto.getSensorThres());
        sensor.setAllowVal(dto.getAllowVal());
        sensorRepository.save(sensor);
    }

    private Zone getZoneById(String zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 공간 ID: " + zoneId));
    }
    private Equip getEquipById(String eqiuipId) {
        return equipRepository.findById(eqiuipId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 센서 ID: " + eqiuipId));
    }
    public Sensor getSensorById(String sensorId) {
        return sensorRepository.findById(sensorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 센서 ID: " + sensorId));
    }
}