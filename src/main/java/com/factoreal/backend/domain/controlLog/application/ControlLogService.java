package com.factoreal.backend.domain.controlLog.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.factoreal.backend.domain.controlLog.dao.ControlLogRepository;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.messaging.mqtt.MqttPublishService;
import com.factoreal.backend.messaging.sender.WebSocketSender;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
// 제어 로그 저장 및 알림 전송 서비스
public class ControlLogService {

    private final ControlLogRepository controlLogRepository;
    private final MqttPublishService mqttPublishService;
    private final WebSocketSender webSocketSender;

    @Transactional
    public ControlLog saveControlLog(AbnormalLog abnormalLog, String controlType, Double controlVal, Integer controlStat,
                                     Zone zone) {
        ControlLog controlLog = ControlLog.builder()
                .abnormalLog(abnormalLog)
                .controlType(controlType)
                .controlVal(controlVal)
                .controlStat(controlStat)
                .executedAt(LocalDateTime.now())
                .zone(zone)
                .build();

        // 제어 로그 저장
        ControlLog savedLog = controlLogRepository.save(controlLog);

        // 발송 여부를 포함할 맵
        Map<String, Boolean> deliveryStatus = new HashMap<>();

        try {
            // MQTT 메시지 발행
            mqttPublishService.publishControlMessage(savedLog);
            deliveryStatus.put("mqttDelivered", true);
        } catch (Exception e) {
            log.error("❌ MQTT 메시지 발행 실패: {}", e.getMessage(), e);
            deliveryStatus.put("mqttDelivered", false);
        }

        try {
            // WebSocket으로 제어 상태 전송 (발송 상태 포함)
            webSocketSender.sendControlStatus(savedLog, deliveryStatus);

            log.info("✅ 제어 로그 저장 및 알림 전송 완료: controlId={}, abnormalId={}, status={}",
                    savedLog.getId(), abnormalLog.getId(), deliveryStatus);
        } catch (Exception e) {
            log.error("❌ WebSocket 메시지 전송 실패: {}", e.getMessage(), e);
        }

        return savedLog;
    }
}
