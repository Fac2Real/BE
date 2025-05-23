package com.factoreal.backend.messaging.config;

import com.factoreal.backend.messaging.common.util.SslUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLSocketFactory;

@Slf4j
@Configuration
public class MqttConfig {
    @Bean
    public MqttClient mqttClient(SslUtil sslUtil) throws Exception {
        // 🟢 AWS IoT 브로커 주소 및 포트 설정
        String broker = "ssl://a2q1cmw33m6k7u-ats.iot.ap-northeast-2.amazonaws.com:8883";
        // 🟢 고유한 MQTT 클라이언트 ID 생성
        String clientId = "SPRING_Dain";
        // 🔐 SSL 인증서 경로 설정
        SSLSocketFactory sslFactory;
        try {
            // AWS Secret Manager에 정의된 secret 식별자 사용
            sslFactory = sslUtil.getSocketFactoryFromSecrets("monitory/dev/iotSecrets");
            log.info("✅Secret Manager에서 Pem키 가져오기 성공!");
        }catch (Exception e){
            // AWS Secret Manager에 Pem이 등록되지 않았다면 그대로 로컬의 pem키 사용.
            log.info("❌Secret Manager에서 Pem키 가져오기 실패 {}", e.getMessage());
            sslFactory = sslUtil.getSocketFactoryFromFiles(
                    "src/main/resources/certs/root.pem",
                    "src/main/resources/certs/e26518c769f6e58cdf25f97a56b948cc340e1e8f5ff053f7188db2bbc4a3b4bf-certificate.pem.crt",
                    "src/main/resources/certs/e26518c769f6e58cdf25f97a56b948cc340e1e8f5ff053f7188db2bbc4a3b4bf-private.pem.key"
            );
            log.info("✅로컬 경로에서 Pem키 가져오기 성공!");
        }
        MqttConnectOptions options = new MqttConnectOptions();
        options.setSocketFactory(sslFactory);
        // 영구 저장소 비활성화
        options.setCleanSession(true);
        // 자동 재연결 설정
        options.setAutomaticReconnect(true);
        // 연결 타임아웃 설정 (5초)
        options.setConnectionTimeout(5);

        MqttClient client = new MqttClient(broker, clientId, null);
        client.connect(options);
        log.info("✅Mqtt 연결 성공!");
        return client;
    }
}
