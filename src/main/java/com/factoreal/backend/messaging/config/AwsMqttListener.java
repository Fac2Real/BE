package com.factoreal.backend.messaging.config;

import com.factoreal.backend.domain.sensor.dto.request.SensorCreateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.factoreal.backend.domain.sensor.application.SensorService;
import com.factoreal.backend.messaging.common.util.SslUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.dao.DataIntegrityViolationException;

import javax.net.ssl.SSLSocketFactory;
import java.nio.charset.StandardCharsets;

/**
 * ✅ AWS IoT Core MQTT 리스너
 * - 사물(센서)의 shadow update 메시지를 구독
 */
@Slf4j
//@Component // 빈에 등록되지 않도록 변경
@RequiredArgsConstructor
public class AwsMqttListener {

    private final SensorService sensorService;
    private final SslUtil sslUtil;
    @PostConstruct
    public void connect() throws Exception {

        // 🟢 AWS IoT 브로커 주소 및 포트 설정
        String broker = "ssl://a2n7kxevn6fh72-ats.iot.ap-northeast-2.amazonaws.com:8883";

        // 🟢 구독할 Thing 설정
        String topic = "#";

        // 🟢 고유한 MQTT 클라이언트 ID 생성
        String clientId = "SPRING_Dain";

        // 🔥 파일 존재 여부 확인 로그 추가
        log.info("✅ root.pem exists: {}", new java.io.File("src/main/resources/certs/root.pem").exists());
        log.info("✅ device-certificate.pem.crt exists: {}", new java.io.File("src/main/resources/certs/54e5d2549e672108375364398317635c85a2a4082c90ff9378d02a118bd41800-certificate.pem.crt").exists());
        log.info("✅ private-key.pem.key exists: {}", new java.io.File("src/main/resources/certs/54e5d2549e672108375364398317635c85a2a4082c90ff9378d02a118bd41800-private.pem.key").exists());


        // 🔐 SSL 인증서 경로 설정
        SSLSocketFactory sslFactory;
        try {
            // AWS Secret Manager에 정의된 secret 식별자 사용
            sslFactory = sslUtil.getSocketFactoryFromSecrets("Secret Manger 식별자");
        }catch (Exception e){
            // AWS Secret Manager에 Pem이 등록되지 않았다면 그대로 로컬의 pem키 사용.
            sslFactory = sslUtil.getSocketFactoryFromFiles(
                    "src/main/resources/certs/root.pem",
                    "src/main/resources/certs/54e5d2549e672108375364398317635c85a2a4082c90ff9378d02a118bd41800-certificate.pem.crt",
                    "src/main/resources/certs/54e5d2549e672108375364398317635c85a2a4082c90ff9378d02a118bd41800-private.pem.key"
            );
        }
        MqttConnectOptions options = new MqttConnectOptions();
        options.setSocketFactory(sslFactory);

        // MQTT 클라이언트 연결
        MqttClient client = new MqttClient(broker, clientId);
        client.connect(options); // ✅ 연결 시도
        log.info("✅ MQTT 연결 완료됨"); // ★ 연결 완료 로그

        // 메시지 구독
        log.info("📡 MQTT subscribe 시작: topic = {}", topic); // ★ subscribe 전에 로그


        // ✅ 메시지 구독 및 처리 (로그 출력만)
//        client.subscribe("#", 1, (t, msg) -> log.info("[#] topic: {}, msg: {}", t, new String(msg.getPayload())));
//        client.subscribe("/#", 1, (t, msg) -> log.info("[/#] topic: {}, msg: {}", t, new String(msg.getPayload())));

        client.subscribe(topic,1,  (t, msg) -> {
            String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
            log.info("📥 MQTT 수신 (topic: {}): {}", t, payload);


            // JSON 파싱 및 DB 저장은 이후 구현 예정
            try{
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(payload);

                String sensorId = jsonNode.at("/id").asText();
                String type = jsonNode.at("/type").asText();
//                SensorDto dto = new SensorDto(sensorId, type);
                SensorCreateRequest dto = new SensorCreateRequest();
                sensorService.saveSensor(dto); // 중복이면 예외 발생
                log.info("✅ 센서 저장 완료: {}", sensorId);
            } catch (DataIntegrityViolationException e) {
                log.warn("⚠️ 중복 센서 저장 시도 차단됨: {}", e.getMessage());
            } catch (Exception e) {
                log.error("❌ JSON 파싱 또는 저장 중 오류: {}", e.getMessage());
            }

        });
        log.info("📡 MQTT subscribe 완료됨: topic = {}", topic); // ★ subscribe 후에도 로그
    }
}

