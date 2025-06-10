package com.factoreal.backend.domain.sensor.application;

import com.factoreal.backend.domain.equip.application.EquipRepoService;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.sensor.dto.request.SensorCreateRequest;
import com.factoreal.backend.domain.sensor.dto.request.SensorUpdateRequest;
import com.factoreal.backend.domain.sensor.dto.response.SensorInfoResponse;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorServiceTest {

    /* ======= Mock 의존 ======= */
    @Mock
    ZoneRepoService zoneRepo;
    @Mock
    EquipRepoService equipRepo;
    @Mock SensorRepoService sensorRepo;

    @InjectMocks
    SensorService sensorService;

    /* ======= 공용 더미 ======= */
    private Zone zone () { return Zone.builder().zoneId("Z1").zoneName("대기실").build(); }
    private Equip equip() { return Equip.builder().equipId("E1").equipName("로봇암").build(); }
    private Sensor sensor(String id) {
        return Sensor.builder()
                .sensorId(id).sensorType(SensorType.temp)
                .zone(zone()).equip(equip())
                .isZone(0).build();
    }
    private SensorInfoResponse sensorInfo(String id) {
        return SensorInfoResponse.builder()
                .sensorId(id)
                .sensorType("temp")
                .zoneId("Z1")
                .equipId("E1")
                .isZone(0)
                .build();
    }

    /* ---------- 1) saveSensor 성공 ---------- */
    @Test
    void saveSensor_success() {
        // given
        SensorCreateRequest dto = new SensorCreateRequest(
                "S-1","temp","Z1","E1", null,null,0);

        when(zoneRepo.findById("Z1")).thenReturn(zone());
        when(equipRepo.findById("E1")).thenReturn(equip());
        when(sensorRepo.save(any(Sensor.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // when
        Sensor saved = sensorService.saveSensor(dto);

        // then
        assertThat(saved.getSensorId()).isEqualTo("S-1");
        verify(sensorRepo).save(any(Sensor.class));
    }

    /* ---------- 2) Zone 미존재 → 404 ---------- */
    @Test
    void saveSensor_zoneNotFound_throws404() {
        SensorCreateRequest dto =
                new SensorCreateRequest("S-2","TEMP","Z999","E1",null,null,0);

        when(zoneRepo.findById("Z999"))
                .thenThrow(new NotFoundException("Zone 없음"));

        assertThrows(NotFoundException.class,
                () -> sensorService.saveSensor(dto));
    }

    /* ---------- 3) getAllSensors ---------- */
    @Test
    void getAllSensors_success() throws JsonProcessingException {
        when(sensorRepo.findAll())
                .thenReturn(List.of(sensorInfo("S-1"), sensorInfo("S-2")));

        List<SensorInfoResponse> list = sensorService.getAllSensors();

        assertThat(list).hasSize(2)
                .extracting(SensorInfoResponse::getSensorId)
                .containsExactlyInAnyOrder("S-1","S-2");
        /* 👁️ 내용 출력 (pretty JSON) */
        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println("=== getAllSensors() 결과 ===");
        System.out.println(om.writeValueAsString(list));
    }

    /* ---------- 4) updateSensor ---------- */
    @Test
    void updateSensor_updatesThreshold() {
        Sensor origin = sensor("S-1");
        when(sensorRepo.getSensorById("S-1")).thenReturn(origin);

        SensorUpdateRequest dto = new SensorUpdateRequest(30.0, 0.5);
        sensorService.updateSensor("S-1", dto);

        assertThat(origin.getSensorThres()).isEqualTo(30.0);
        assertThat(origin.getAllowVal()).isEqualTo(0.5);
        verify(sensorRepo).save(origin);
    }
}