package com.factoreal.backend.messaging.mqtt;

import com.factoreal.backend.domain.sensor.application.SensorService;
import com.factoreal.backend.domain.sensor.dto.request.SensorCreateRequest;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    /**
     * subscribe 를 목킹하면서 원하는 payload 로 콜백을 "직접" 실행하는 유틸
     * */
    private void stubSubscribeWithPayload(String payload) throws Exception {
        doAnswer(inv -> {
            IMqttMessageListener listener = inv.getArgument(2);
            listener.messageArrived("topic", new MqttMessage(payload.getBytes(StandardCharsets.UTF_8)));
            return null;
        }).when(mqtt).subscribe(anyString(), anyInt(), any(IMqttMessageListener.class));
    }

    /* ========== 1) equipId Null 로 들어올 경우에 대한 처리 isZone = 0 ========== */
    @Test
    void equipId_absent_sets_isZone_zero() throws Exception {
        when(mqtt.isConnected()).thenReturn(true);

        String payload = """
        {"current":{"state":{"reported":{
            "sensorId":"S-101","type":"temp","zoneId":"Z1","equipId":""}}}}
        """;
        stubSubscribeWithPayload(payload);

        mqttSvc.SensorShadowSubscription();

        ArgumentCaptor<SensorCreateRequest> cap = ArgumentCaptor.forClass(SensorCreateRequest.class);
        verify(sensorService).saveSensor(cap.capture());
        SensorCreateRequest dto = cap.getValue();

        assertThat(dto.getEquipId()).isNull();
        assertThat(dto.getIsZone()).isEqualTo(0);
    }

    /* ========== 2) equipId = zoneId 이면 iszone = 1 (환경 센서) ========== */
    @Test
    void equipId_equals_zoneId_sets_isZone_one() throws Exception {
        when(mqtt.isConnected()).thenReturn(true);

        String payload = """
        {"current":{"state":{"reported":{
            "sensorId":"S-102","type":"temp","zoneId":"Z1","equipId":"Z1"}}}}
        """;
        stubSubscribeWithPayload(payload);

        mqttSvc.SensorShadowSubscription();

        ArgumentCaptor<SensorCreateRequest> cap = ArgumentCaptor.forClass(SensorCreateRequest.class);
        verify(sensorService).saveSensor(cap.capture());
        assertThat(cap.getValue().getIsZone()).isEqualTo(1);
    }

    /* ========== 3) zoneId 가 비면 저장 시도 자체가 없다 ========== */
    @Test
    void blank_zoneId_is_ignored() throws Exception {
        when(mqtt.isConnected()).thenReturn(true);

        String payload = """
        {"current":{"state":{"reported":{
            "sensorId":"S-103","type":"temp","zoneId":""}}}}
        """;
        stubSubscribeWithPayload(payload);

        mqttSvc.SensorShadowSubscription();

        verify(sensorService, never()).saveSensor(any());
    }

    /* ========== 4) saveSensor 중복 오류(DataIntegrityViolationException)도 안전 처리 ========== */
    @Test
    void dataIntegrityViolation_is_caught() throws Exception {
        when(mqtt.isConnected()).thenReturn(true);

        String payload = """
        {"current":{"state":{"reported":{
            "sensorId":"S-104","type":"temp","zoneId":"Z1"}}}}
        """;
        stubSubscribeWithPayload(payload);

        doThrow(new DataIntegrityViolationException("dup")).when(sensorService).saveSensor(any());

        // 예외가 밖으로 전파되지 않고 처리 되는지만 확인
        mqttSvc.SensorShadowSubscription();
        verify(sensorService).saveSensor(any());
    }

    /* ========== 5) JSON 파싱 실패 등 일반 예외도 안전 처리 ========== */
    @Test
    void malformed_json_is_caught() throws Exception {
        when(mqtt.isConnected()).thenReturn(true);

        // 일부러 중괄호 누락 → 파싱 오류 유발
        String badJson = "{\"current\":{\"state\":{\"reported\":{";
        stubSubscribeWithPayload(badJson);

        mqttSvc.SensorShadowSubscription();

        verify(sensorService, never()).saveSensor(any());
    }

    /* ========== 6) 초기 미연결 상태면 reconnect 후 subscribe 재시도 ========== */
    @Test
    void reconnect_when_not_connected_first() throws Exception {
        when(mqtt.isConnected()).thenReturn(false, true); // 첫 호출 F, 두 번째 T

        // reconnect() 는 바로 true 를 돌려주도록 목킹
        doNothing().when(mqtt).reconnect();

        String payload = """
        {"current":{"state":{"reported":{
            "sensorId":"S-105","type":"temp","zoneId":"Z1"}}}}
        """;
        stubSubscribeWithPayload(payload);

        mqttSvc.SensorShadowSubscription();

        verify(mqtt).reconnect();          // 재연결 한 번 시도
        verify(sensorService).saveSensor(any());
        /* 👁️ JSON 형태로 Dto출력 */
        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println("=== mqtt DTO ===");
        System.out.println(om.writeValueAsString(mqtt));
    }
    /* ========== 7) zoneId 가 null 이면 saveSensor() 호출 X ========== */
    @Test
    void zoneId_null_is_ignored() throws Exception {
        when(mqtt.isConnected()).thenReturn(true);

        String payload = """
      {"current":{"state":{"reported":{
        "sensorId":"S-301","type":"temp"
      }}}}
      """;
        stubSubscribeWithPayload(payload);        // util 메서드(앞서 정의)

        mqttSvc.SensorShadowSubscription();

        verify(sensorService, never()).saveSensor(any());
    }

    /* 인터럽트 플래그 초기화 */
    @SuppressWarnings("ResultOfMethodCallIgnored") // 의도적으로 결과 무시
    private void clearInterruptFlag() {
        Thread.interrupted();
    }

    /* ========== 8) 초기 reconnect 직후 sleep() 에서 인터럽트 → 외부 catch 검증 ========== */
    @Test
    void interrupt_during_initial_sleep_throws_mqttException() throws Exception {
        // 1️⃣ 연결 안 됨 → reconnect() → sleep() 경로로 진입
        when(mqtt.isConnected()).thenReturn(false);
        doNothing().when(mqtt).reconnect();

        // 2️⃣ sleep() 전에 인터럽트 플래그 세팅 → 즉시 InterruptedException 발생
        Thread.currentThread().interrupt();

        assertThrows(MqttException.class, () -> mqttSvc.SensorShadowSubscription());

        // 3️⃣ 메서드 안에서 다시 interrupt() 했으므로 아직 true
        assertTrue(Thread.currentThread().isInterrupted());

        // 4️⃣ 다음 테스트에 영향 없도록 플래그 클리어
        clearInterruptFlag();
    }

    /* ========== 9) subscribe 실패→ catch 블록 내부 sleep() 도중 인터럽트 → 재래핑 검증 ========== */
    @Test
    void interrupt_during_retry_sleep_still_throws_mqttException() throws Exception {
        when(mqtt.isConnected()).thenReturn(true);                   // 바로 subscribe
        doThrow(new MqttException(0)).when(mqtt)
                .subscribe(anyString(), anyInt(), any());            // subscribe 실패

        Thread.currentThread().interrupt();                          // retry sleep 직전 플래그 ON
        assertThrows(MqttException.class, () -> mqttSvc.SensorShadowSubscription());

        // (옵션) subscribe 는 정확히 1번만 시도됨
        verify(mqtt, times(1)).subscribe(anyString(), anyInt(), any());

        clearInterruptFlag();                                    // 플래그 초기화
    }
    /* ========== 9) subscribe 실패→ catch 블록 내부 sleep() 도중 인터럽트 → 재래핑 검증 ========== */
    @Test
    void interrupt_during_retry_6times_throws_mqttException() throws Exception {
        when(mqtt.isConnected()).thenReturn(true);                   // 바로 subscribe
        doThrow(new MqttException(0)).when(mqtt)
                .subscribe(anyString(), anyInt(), any());            // subscribe 실패
//        MqttException ex = new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION);

// subscribe() 호출 때 예외 발생하도록 세팅
//        doThrow(ex).when(mqtt)
//                .subscribe(eq("sensor/temp"), eq(1), any(IMqttMessageListener.class));
//        IMqttMessageListener listener = mock(IMqttMessageListener.class);
////        Thread.currentThread().interrupt();                          // retry sleep 직전 플래그 ON
//        Exception MqttException = new MqttException(0);
        MqttException ex = new MqttException(1);

// subscribe() 호출 때 예외 발생하도록 세팅
//        doThrow(ex).when(mqtt)
//                .subscribe(eq("sensor/temp"), eq(1), any(IMqttMessageListener.class));

        //        when(mqtt.subscribe(anyString(),any(),listener)).thenThrow(MqttException);
//        assertThrows(MqttException.class, () -> mqttSvc.SensorShadowSubscription());
        mqttSvc.SensorShadowSubscription();
        // (옵션) subscribe 는 정확히 1번만 시도됨
        verify(mqtt, times(5)).subscribe(anyString(), anyInt(), any());

//        clearInterruptFlag();                                    // 플래그 초기화
    }

//    /* ========== 10) 재시도 5회 초과 시 에러로그 호출 ========== */
//    @Test
//    void errorLog_called_when_retry_exceeds_max_attempts() throws Exception {
//        when(mqtt.isConnected()).thenReturn(false); // 항상 연결 안 됨
//        doThrow(new org.eclipse.paho.client.mqttv3.MqttException(0)).when(mqtt).reconnect();
//
//        // log.error를 감시하기 위해 spy 사용
//        MqttService spySvc = Mockito.spy(new MqttService(mqtt, sensorService));
//        // log 필드를 spy로 교체
//        java.lang.reflect.Field logField = MqttService.class.getDeclaredField("log");
//        logField.setAccessible(true);
//        org.slf4j.Logger loggerSpy = mock(org.slf4j.Logger.class);
//        logField.set(null, loggerSpy);
//
//        try {
//            spySvc.SensorShadowSubscription();
//        } catch (org.eclipse.paho.client.mqttv3.MqttException ignored) {}
//
//        // 5회 이상 실패 시 log.error가 호출되는지 검증
//        verify(loggerSpy, atLeastOnce()).error(anyString());
//    }
}