package com.factoreal.backend.messaging.mqtt;

import com.factoreal.backend.domain.sensor.application.SensorService;
import com.factoreal.backend.domain.sensor.dto.request.SensorCreateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttService {
    private final MqttClient mqttClient;
    private final SensorService sensorService;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 2000;

    /**
     * - 디바이스의 shadow 메타데이터 변경사항(등록/수정)을 구독
     * @throws MqttException mqtt 연결에 실패시 MqttException 발생
     */
    @PostConstruct
    public void SensorShadowSubscription() throws MqttException {
        int retryCount = 0;
        while (retryCount < MAX_RETRY_ATTEMPTS) {
            try {
                if (!mqttClient.isConnected()) {
                    log.warn("MQTT 클라이언트가 연결되어 있지 않습니다. 재연결 시도 중... (시도 {}/{})", 
                            retryCount + 1, MAX_RETRY_ATTEMPTS);
                    mqttClient.reconnect();
                    Thread.sleep(RETRY_DELAY_MS);
                }

                // 🟢 구독할 Thing 설정
                String thingName = "Sensor";
                // #는 topic의 여러 level을 대체 가능, +는 topic의 단일 level을 대체 가능
                String topic = "$aws/things/" + thingName + "/shadow/name/+/update/documents";
                
                mqttClient.subscribe(topic, 1, (t, msg) -> {
                    String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);

                    // JSON 파싱 및 DB 저장은 이후 구현 예정
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(payload);
                        // mqtt에서 전달되는 뎁스를 따라가야함
                        JsonNode reported = jsonNode.at("/current/state/reported");
                        log.info("📥 MQTT 수신 (topic: {}): {}", t, jsonNode);

                        String sensorId = reported.at("/sensorId").asText();
                        String type = reported.at("/type").asText();
                        String zoneId = reported.at("/zoneId").asText();
                        /* ---------- equipId / equipName 처리 ---------- */
                        String equipIdVal = reported.path("equipId").asText(null);   // 키가 없으면 null
                        String equipId = (equipIdVal == null || equipIdVal.isBlank()) ? null : equipIdVal;

                        if (zoneId.isBlank()) {
                            log.error("❌ 유효하지 않은 zoneId: {}", zoneId);
                            return;
                        }

                        Integer iszone = (equipId != null && equipId.equals(zoneId)) ? 1 : 0;

                        SensorCreateRequest dto = new SensorCreateRequest(sensorId, type, zoneId, equipId, null, null, iszone);
                        sensorService.saveSensor(dto); // 중복이면 예외 발생
                        log.info("✅ 센서 저장 완료: {}", sensorId);
                    } catch (DataIntegrityViolationException e) {
                        log.warn("⚠️ 중복 센서 저장 시도 차단됨: {}", e.getMessage());
                    } catch (Exception e) {
                        log.error("❌ JSON 파싱 또는 저장 중 오류: {}", e.getMessage());
                    }
                });
                
                log.info("📡 MQTT subscribe 완료됨: topic = {}", topic);
                return; // 성공적으로 구독했으면 메서드 종료
                
            } catch (MqttException e) {
                log.error("MQTT 연결/구독 실패 (시도 {}/{}): {}", 
                        retryCount + 1, MAX_RETRY_ATTEMPTS, e.getMessage());
                retryCount++;
                if (retryCount >= MAX_RETRY_ATTEMPTS) {
                    log.error(e.getMessage());
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new MqttException(e.getReasonCode());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION);
            }
        }
    }
}
