package com.factoreal.backend.domain.sensor.dao;

import java.util.Optional;

import com.factoreal.backend.domain.zone.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import com.factoreal.backend.domain.sensor.entity.Sensor;

import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, String> {
    Optional<Sensor> findBySensorId(String sensorId);
    List<Sensor> findByZone(Zone zone);
}