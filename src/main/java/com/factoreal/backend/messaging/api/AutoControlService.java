package com.factoreal.backend.messaging.api;

import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.sensor.entity.Sensor;

import jakarta.transaction.Transactional;

import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.controlLog.application.ControlLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * [공간센서제어]
 * 공간 센서 측정값이 임계치를 벗어났는지 판단하여 자동 제어가 필요한 상황을 감지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoControlService {

    //    private final SensorService sensorService;
    private final ControlLogService controlLogService;
    private final SensorRepoService sensorRepoService;

    /**
     * 센서 값이 허용 범위를 벗어났을 경우 제어 메시지를 생성하거나 처리하도록 로깅
     */
    @Transactional
    public void evaluate(SensorKafkaDto dto, AbnormalLog abnormalLog, int dangerLevel) {
        if (dangerLevel == 0)
            return; // 정상 범위면 아무 처리 안 함

        // dto나 sensorId가 null인 경우 처리
        if (dto == null || dto.getSensorId() == null) {
            log.warn("❌ 유효하지 않은 센서 데이터: dto={}", dto);
            return;
        }

        Sensor sensor = sensorRepoService.findById(dto.getSensorId());
        if (sensor == null) {
            log.warn("❌ 센서 정보 조회 실패: sensorId={}", dto.getSensorId());
            return;
        }
        if (sensor.getSensorThres() == null) {
            log.warn("❌ 센서 임계치 없음 -> 자동 제어 스킵");
            return;
        }

        // 센서값이 null인 경우 처리
        Double sensorValue = dto.getVal();
        if (sensorValue == null) {
            log.warn("❌ 센서 측정값이 null입니다: sensorId={}", dto.getSensorId());
            return;
        }

        double threshold = sensor.getSensorThres();
        double tolerance = sensor.getAllowVal() != null ? sensor.getAllowVal() : 0.0;
        double value = sensorValue;

        // 센서 값이 허용 범위를 벗어났을 경우
        if (value < threshold - tolerance || value > threshold + tolerance) {
            String message = buildControlMessage(sensor.getSensorType().name(), value, threshold, tolerance);
            log.info("⚙️ 자동제어 필요: {}", message);

            // 2. 제어 로그 저장
            String controlType = getControlType(sensor.getSensorType().name());

            controlLogService.saveControlLog(
                    abnormalLog,
                    controlType,
                    threshold, // controlVal: 임계값을 목표값으로 사용
                    1, // controlStat: 성공 상태로 설정
                    sensor.getZone());
        } else {
            log.info("✅ 측정값은 허용 범위 내: sensorId={}, value={}", dto.getSensorId(), value);
        }
    }

    // 제어 로직
    private String buildControlMessage(String type, double val, double thresh, double tol) {
        return switch (type.toLowerCase()) {
            case "temp" -> String.format("현재 온도 %.1f℃, 적정 범위: %.1f~%.1f℃", val, thresh - tol, thresh + tol);
            case "humid" -> String.format("현재 습도 %.1f%%, 적정 범위: %.1f~%.1f%%", val, thresh - tol, thresh + tol);
            case "vibration" -> String.format("현재 진동 %.1fmm/s, 허용 범위: %.1f~%.1fmm/s", val, thresh - tol, thresh + tol);
            case "current" -> String.format("현재 전류 %.1fmA, 허용 범위: %.1f~%.1fmA", val, thresh - tol, thresh + tol);
            case "dust" -> String.format("현재 미세먼지 %.1f㎍/㎥, 허용 범위: %.1f~%.1f㎍/㎥", val, thresh - tol, thresh + tol);
            default -> String.format("현재 값 %.1f, 허용 범위: %.1f~%.1f", val, thresh - tol, thresh + tol);
        };
    }

    // 센서 타입에 따른 제어 타입 결정
    private String getControlType(String sensorType) {
        return switch (sensorType.toLowerCase()) {
            case "temp" -> "에어컨";
            case "humid" -> "제습기";
            case "dust" -> "공기청정기";
            default -> sensorType;
        };
    }
}
