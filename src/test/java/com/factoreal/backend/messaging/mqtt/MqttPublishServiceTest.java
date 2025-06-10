package com.factoreal.backend.messaging.mqtt;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttPublishServiceTest {

    @Mock
    MqttClient mqttClient;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    MqttPublishService mqttPublishService;

    private ControlLog controlLog;

    @BeforeEach
    void setUp() {
        AbnormalLog abnormalLog = AbnormalLog.builder()
            .id(1L)
            .abnormalType("과열")
            .abnVal(85.5)
            .targetId("sensor-1")
            .targetType(TargetType.Sensor)
            .build();

        Zone zone = Zone.builder()
            .zoneId("zone-001")
            .zoneName("제어구역")
            .build();

        controlLog = ControlLog.builder()
            .id(100L)
            .controlType("FAN")
            .controlVal(1.0)
            .controlStat(1)
            .executedAt(LocalDateTime.of(2025, 6, 10, 12, 0))
            .zone(zone)
            .abnormalLog(abnormalLog)
            .build();
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("정상적으로 MQTT 메시지가 발행된다")
        void testPublishControlMessageSuccess() throws Exception {
            // given
            String expectedJson = "{\"mock\":\"json\"}";
            when(objectMapper.writeValueAsString(anyMap())).thenReturn(expectedJson);

            // when
            mqttPublishService.publishControlMessage(controlLog);

            // then
            String expectedTopic = "control/sensor/sensor-1";
            ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);

            verify(mqttClient, times(1)).publish(eq(expectedTopic), messageCaptor.capture());
            assertEquals(expectedJson, new String(messageCaptor.getValue().getPayload()));
            assertEquals(1, messageCaptor.getValue().getQos());
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("JSON 직렬화 중 예외 발생 시 publish가 호출되지 않는다")
        void testJsonSerializationFailure() throws Exception {
            // given
            JsonProcessingException jsonException = new JsonProcessingException("직렬화 오류") {};
            when(objectMapper.writeValueAsString(anyMap())).thenThrow(jsonException);

            // when
            mqttPublishService.publishControlMessage(controlLog);

            // then
            verify(mqttClient, never()).publish(anyString(), any(MqttMessage.class));
        }
    }
}