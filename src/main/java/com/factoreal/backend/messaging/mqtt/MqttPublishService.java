package com.factoreal.backend.messaging.mqtt;

import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttPublishService {
  private final MqttClient mqttClient;
  private final ObjectMapper objectMapper;

  public void publishControlMessage(ControlLog controlLog) {
    try {
      // MQTT 메시지 페이로드 구성
      Map<String, Object> payload = new HashMap<>();
      payload.put("controlId", controlLog.getId());
      payload.put("controlType", controlLog.getControlType());
      payload.put("controlValue", controlLog.getControlVal());
      payload.put("controlStatus", controlLog.getControlStat());
      payload.put("executedAt", controlLog.getExecutedAt().toString());
      payload.put("zoneId", controlLog.getZone().getZoneId());

      // AbnormalLog 정보 추가
      payload.put("abnormalId", controlLog.getAbnormalLog().getId());
      payload.put("abnormalType", controlLog.getAbnormalLog().getAbnormalType());
      payload.put("abnormalValue", controlLog.getAbnormalLog().getAbnVal());

      // 토픽 구성 - control/{targetType}/{targetId} 형식
      String targetType = controlLog.getAbnormalLog().getTargetType().name().toLowerCase();
      String targetId = controlLog.getAbnormalLog().getTargetId();
      String topic = String.format("control/%s/%s", targetType, targetId);

      // JSON 변환 및 메시지 발행
      String jsonPayload = objectMapper.writeValueAsString(payload);
      MqttMessage message = new MqttMessage(jsonPayload.getBytes());
      message.setQos(1); // QoS 레벨 설정 (최소 1회 전달 보장)

      mqttClient.publish(topic, message);
      log.info("✅ MQTT 제어 메시지 발행 완료: topic={}, payload={}", topic, jsonPayload);

    } catch (Exception e) {
      log.error("❌ MQTT 메시지 발행 실패: {}", e.getMessage(), e);
    }
  }
}
