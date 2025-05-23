package com.factoreal.backend.domain.sensor.api;

import com.factoreal.backend.domain.sensor.dto.response.SensorInfoResponse;
import com.factoreal.backend.domain.sensor.application.SensorService;
import com.factoreal.backend.domain.sensor.dto.request.SensorUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
@Tag(name = "센서 정보 API", description = "센서 정보 처리 API입니다.")
public class SensorController {
    private final SensorService service;

    @GetMapping
    @Operation(summary = "전체 센서 리스트 조회", description = "센서 ID, 센서 종류를 포함한 전체 센서 정보를 조회하는 기능")
    public List<SensorInfoResponse> getSensorList() {
        return service.getAllSensors();
    }

    @PostMapping("/{sensorId}")
    @Operation(summary = "센서 정보 업데이트", description = "센서ID 매핑해서 임계치(sensorThres)와 허용치(allowVal) 업데이트 (FE -> BE) ")
    public ResponseEntity<Void> update(
            @PathVariable("sensorId") String sensorId,
            @RequestBody SensorUpdateRequest sensorUpdateRequest) {
        service.updateSensor(sensorId, sensorUpdateRequest);
        return ResponseEntity.ok().build();
    }
}