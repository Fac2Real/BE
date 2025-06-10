package com.factoreal.backend.messaging.mqtt;

import com.factoreal.backend.domain.sensor.application.SensorService;
import com.factoreal.backend.domain.sensor.dto.request.SensorCreateRequest;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttServiceTest {

    @Mock
    MqttClient mqtt;
    @Mock
    SensorService sensorService;
    @InjectMocks
    MqttService mqttSvc;

    /* MQTT subscribe 직후, 콜백을 직접 호출해 saveSensor 가 불렸는지만 확인 */
    @Test
    void mqttPayload_parsed_andSaved() throws Exception {

        /* 1) MQTT 연결은 돼 있다고 가정 */
        when(mqtt.isConnected()).thenReturn(true);

        /* 2) subscribe → 콜백 강제 실행 */
        doAnswer(inv -> {
            // topic / qos / listener 순으로 넘어온다
            IMqttMessageListener listener = inv.getArgument(2);

            String payload = """
            {
              "current": {
                "state": {
                  "reported": {
                    "sensorId":"S-100",
                    "type":"temp",
                    "zoneId":"Z1",
                    "equipId":"Z1"
                  }
                }
              }
            }
            """;
            listener.messageArrived(
                    "$aws/things/Sensor/shadow/…",
                    new MqttMessage(payload.getBytes(StandardCharsets.UTF_8))
            );
            return null;   // subscribe 의 반환값 → void
        }).when(mqtt).subscribe(anyString(), anyInt(), any(IMqttMessageListener.class));

        /* 3) 실행 */
        mqttSvc.SensorShadowSubscription();

        // 4) 검증 + 인수 캡처
        ArgumentCaptor<SensorCreateRequest> cap = ArgumentCaptor.forClass(SensorCreateRequest.class);
        verify(sensorService).saveSensor(cap.capture());

        SensorCreateRequest dto = cap.getValue();

        assertThat(dto.getSensorId()).isEqualTo("S-100");
        assertThat(dto.getSensorType()).isEqualTo("temp");
        assertThat(dto.getZoneId()).isEqualTo("Z1");
        assertThat(dto.getEquipId()).isEqualTo("Z1");

        /* 👁️ JSON 형태로 Dto출력 */
        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println("=== saveSensor() 호출 DTO ===");
        System.out.println(om.writeValueAsString(dto));
    }
}