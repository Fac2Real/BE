package com.factoreal.backend.messaging.fcm.application;

import static org.junit.jupiter.api.Assertions.*;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.equip.application.EquipRepoService;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.notifyLog.dto.TriggerType;
import com.factoreal.backend.domain.notifyLog.application.NotifyLogService;
import com.factoreal.backend.domain.notifyLog.entity.NotifyLog;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.worker.application.WorkerRepoService;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.application.ZoneHistoryRepoService;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.global.exception.dto.BadRequestException;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.google.firebase.messaging.FirebaseMessagingException;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FCMServiceTest {

    @Mock
    private FCMPushService fcmPushService; // Renamed from fcmService in class to fcmPushService for clarity with field name
    @Mock
    private WorkerRepoService workerRepoService;
    @Mock
    private ZoneHistoryRepoService zoneHistoryRepoService;
    @Mock
    private EquipRepoService equipRepoService;
    @Mock
    private ZoneRepoService zoneRepoService;
    @Mock
    private AbnormalLogRepoService abnormalLogRepoService;
    @Mock
    private NotifyLogService notifyLogService;
    @Mock
    private SensorRepoService sensorRepoService;

    @InjectMocks
    private FCMService fcmService; // The class under test

    private Worker mockWorker;
    private Equip mockEquip;
    private Zone mockZone;
    private ZoneHist mockZoneHist;
    private AbnormalLog mockAbnormalLog;
    private Sensor mockSensor;
    String message;

    @BeforeEach
    void setUp() {
        mockWorker = new Worker();
        mockWorker.setWorkerId("worker123");
        mockWorker.setName("Test Worker");
        mockWorker.setFcmToken("test-fcm-token");

        mockZone = new Zone();
        mockZone.setZoneId("zone1");
        mockZone.setZoneName("Test Zone");

        mockEquip = new Equip();
        mockEquip.setEquipId("equip123");
        mockEquip.setEquipName("Test Equip");
        mockEquip.setZone(mockZone);

        mockZoneHist = new ZoneHist();
        mockZoneHist.setWorker(mockWorker);
        mockZoneHist.setZone(mockZone);

        mockAbnormalLog = new AbnormalLog();
        mockAbnormalLog.setId(1L);
        mockAbnormalLog.setTargetId("sensor1");
        mockAbnormalLog.setTargetType(TargetType.Sensor);

        mockSensor = new Sensor();
        mockSensor.setSensorId("sensor1");
        mockSensor.setSensorType(SensorType.temp);

        message = "test Message";
    }

    @Test
    @DisplayName("saveToken 성공 - Worker를 찾고 FCM 토큰을 저장하고 반환한다")
    void saveToken_success() throws Exception {
        // Arrange
        String workerId = "worker123";
        String token = "new-fcm-token";
        Worker foundWorker = new Worker();
        foundWorker.setWorkerId(workerId);

        Worker savedWorker = new Worker();
        savedWorker.setWorkerId(workerId);
        savedWorker.setFcmToken(token);

        when(workerRepoService.findById(workerId)).thenReturn(foundWorker);
        when(workerRepoService.save(any(Worker.class))).thenReturn(savedWorker);

        // Act
        String resultToken = fcmService.saveToken(workerId, token);

        // Assert
        assertEquals(token, resultToken);
        verify(workerRepoService).findById(workerId);
        ArgumentCaptor<Worker> workerCaptor = ArgumentCaptor.forClass(Worker.class);
        verify(workerRepoService).save(workerCaptor.capture());
        assertEquals(token, workerCaptor.getValue().getFcmToken());
    }

    @Test
    @DisplayName("saveToken 실패 - Worker를 찾지 못하면 예외를 던진다")
    void saveToken_workerNotFound_throwsException() {
        // Arrange
        String workerId = "unknownWorker";
        String token = "some-token";
        when(workerRepoService.findById(workerId)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            fcmService.saveToken(workerId, token);
        });
        assertEquals("Worker not found", exception.getMessage());
        verify(workerRepoService).findById(workerId);
        verify(workerRepoService, never()).save(any(Worker.class));
    }

    @Test
    @DisplayName("sendEquipMaintain 성공 - FCM 메시지를 보내고 NotifyLog를 저장한다")
    void sendEquipMaintain_success() throws ExecutionException, InterruptedException, FirebaseMessagingException {
        // Arrange
        String workerId = "worker123";
        String equipId = "equip123";

        when(equipRepoService.findById(equipId)).thenReturn(mockEquip);
        when(workerRepoService.findById(workerId)).thenReturn(mockWorker);
        when(fcmPushService.sendMessage(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("messageId"));

        // Act
        fcmService.sendEquipMaintain(workerId, equipId, message);

        // Assert
        verify(equipRepoService).findById(equipId);
        verify(workerRepoService).findById(workerId);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fcmPushService).sendMessage(tokenCaptor.capture(), titleCaptor.capture(), bodyCaptor.capture());

        assertEquals(mockWorker.getFcmToken(), tokenCaptor.getValue());
        assertEquals("[확인] 수동 호출, 설비 점검 요청", titleCaptor.getValue());
        assertEquals("Test Zone의 Test Equip설비 점검 요청합니다.\n[관리자 메세지] test Message", bodyCaptor.getValue());

        verify(notifyLogService).saveNotifyLogFromFCM(
                eq(workerId),
                eq(Boolean.TRUE),
                eq(TriggerType.MANUAL),
                any(LocalDateTime.class),
                isNull()
        );
    }

    @Test
    @DisplayName("sendWorkerSafety 성공 - ZoneHist 있을 때 FCM 메시지를 보내고 NotifyLog를 저장한다")
    void sendWorkerSafety_success_withZoneHist() throws ExecutionException, InterruptedException, FirebaseMessagingException {
        // Arrange
        String helperWorkerId = "helper123";
        String careNeedWorkerId = "careWorker456";

        Worker helperWorker = new Worker();
        helperWorker.setWorkerId(helperWorkerId);
        helperWorker.setFcmToken("helper-token");

        Worker careNeedWorker = new Worker();
        careNeedWorker.setWorkerId(careNeedWorkerId);
        careNeedWorker.setName("Care Worker");

        ZoneHist currentZoneHist = new ZoneHist();
        Zone currentZone = new Zone();
        currentZone.setZoneName("Danger Zone");
        currentZoneHist.setZone(currentZone);

        when(workerRepoService.findById(helperWorkerId)).thenReturn(helperWorker);
        when(workerRepoService.findById(careNeedWorkerId)).thenReturn(careNeedWorker);
        when(zoneHistoryRepoService.getCurrentWorkerLocation(careNeedWorkerId)).thenReturn(currentZoneHist);
        when(fcmPushService.sendMessage(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("messageId"));

        // Act
        fcmService.sendWorkerSafety(helperWorkerId, careNeedWorkerId,message);

        // Assert
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fcmPushService).sendMessage(eq(helperWorker.getFcmToken()), anyString(), bodyCaptor.capture());
        assertEquals("Danger Zone에 있는 작업자 Care Worker씨의 건강 이상이 발견되었습니다. 지원바랍니다.\n[관리자 메세지] test Message", bodyCaptor.getValue());

        verify(notifyLogService).saveNotifyLogFromFCM(
                eq(helperWorkerId),
                eq(Boolean.TRUE),
                eq(TriggerType.MANUAL),
                any(LocalDateTime.class),
                isNull()
        );
    }

    @Test
    @DisplayName("sendWorkerSafety 성공 - ZoneHist 없을 때 기본 Zone 이름으로 FCM 메시지를 보내고 NotifyLog를 저장한다")
    void sendWorkerSafety_success_withoutZoneHist() throws ExecutionException, InterruptedException, FirebaseMessagingException {
        // Arrange
        String helperWorkerId = "helper123";
        String careNeedWorkerId = "careWorker456";

        Worker helperWorker = new Worker();
        helperWorker.setWorkerId(helperWorkerId);
        helperWorker.setFcmToken("helper-token");

        Worker careNeedWorker = new Worker();
        careNeedWorker.setWorkerId(careNeedWorkerId);
        careNeedWorker.setName("Care Worker");

        when(workerRepoService.findById(helperWorkerId)).thenReturn(helperWorker);
        when(workerRepoService.findById(careNeedWorkerId)).thenReturn(careNeedWorker);
        when(zoneHistoryRepoService.getCurrentWorkerLocation(careNeedWorkerId)).thenReturn(null); // No ZoneHist
        when(fcmPushService.sendMessage(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("messageId"));

        // Act
        fcmService.sendWorkerSafety(helperWorkerId, careNeedWorkerId,message);

        // Assert
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fcmPushService).sendMessage(eq(helperWorker.getFcmToken()), anyString(), bodyCaptor.capture());
        assertEquals(FCMService.DEFAULT_ZONE_NAME + "에 있는 작업자 Care Worker씨의 건강 이상이 발견되었습니다. 지원바랍니다.\n[관리자 메세지] test Message", bodyCaptor.getValue());

        verify(notifyLogService).saveNotifyLogFromFCM(
                eq(helperWorkerId),
                eq(Boolean.TRUE),
                eq(TriggerType.MANUAL),
                any(LocalDateTime.class),
                isNull()
        );
    }

    @Nested
    @DisplayName("공간에 위험이 있는 경우, 마지막 위험을 공간내 작업자에게 알려주는 메서드")
    class sendZoneSafety {

    }

    @Test
    @DisplayName("sendZoneSafety 성공 - abnormalLogOption null일 때, Zone 내 작업자들에게 FCM 전송 및 NotifyLog 저장")
    void sendZoneSafety_success_abnormalLogNull() throws ExecutionException, InterruptedException, FirebaseMessagingException {
        // Arrange
        String zoneId = "zone1";
        Integer dangerLevel = 2;
        TriggerType triggerType = TriggerType.AUTOMATIC;
        LocalDateTime time = LocalDateTime.now();

        Worker workerInZone1 = new Worker();
        workerInZone1.setWorkerId("workerInZone1");
        workerInZone1.setFcmToken("token1");
        ZoneHist zoneHist1 = new ZoneHist();
        zoneHist1.setWorker(workerInZone1);

        Worker workerInZone2 = new Worker();
        workerInZone2.setWorkerId("workerInZone2");
        workerInZone2.setFcmToken("token2");
        ZoneHist zoneHist2 = new ZoneHist();
        zoneHist2.setWorker(workerInZone2);

        List<ZoneHist> zoneHists = List.of(zoneHist1, zoneHist2);

        when(zoneRepoService.findById(zoneId)).thenReturn(mockZone);
        when(abnormalLogRepoService.findLatestSensorLogInZoneWithDangerLevel(TargetType.Sensor, mockZone, dangerLevel))
                .thenReturn(mockAbnormalLog);
        when(sensorRepoService.getSensorById(mockAbnormalLog.getTargetId())).thenReturn(mockSensor);
        when(zoneHistoryRepoService.getCurrentWorkersByZoneId(zoneId)).thenReturn(zoneHists);
        when(fcmPushService.sendMessage(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("messageId"));

        // Act
        fcmService.sendZoneSafety(zoneId, dangerLevel, triggerType, time, null);

        // Assert
        verify(zoneRepoService).findById(zoneId);
        verify(abnormalLogRepoService).findLatestSensorLogInZoneWithDangerLevel(TargetType.Sensor, mockZone, dangerLevel);
        verify(sensorRepoService).getSensorById(mockAbnormalLog.getTargetId());
        verify(zoneHistoryRepoService).getCurrentWorkersByZoneId(zoneId);

        String expectedTitle = "[주의] 수동 호출, 작업장 위험"; // Title is fixed in code
        String expectedBody = "Test Zone에 있는 작업자들은 temp 센서의 수치가 높으므로 주의하세요.";

        verify(fcmPushService, times(2)).sendMessage(anyString(), eq(expectedTitle), eq(expectedBody));
        verify(fcmPushService).sendMessage(eq(workerInZone1.getFcmToken()), eq(expectedTitle), eq(expectedBody));
        verify(fcmPushService).sendMessage(eq(workerInZone2.getFcmToken()), eq(expectedTitle), eq(expectedBody));

        verify(notifyLogService, times(2)).saveNotifyLogFromFCM(
                anyString(),
                eq(Boolean.TRUE),
                eq(triggerType),
                eq(time),
                eq(mockAbnormalLog.getId())
        );
        verify(notifyLogService).saveNotifyLogFromFCM(eq(workerInZone1.getWorkerId()), eq(Boolean.TRUE), eq(triggerType), eq(time), eq(mockAbnormalLog.getId()));
        verify(notifyLogService).saveNotifyLogFromFCM(eq(workerInZone2.getWorkerId()), eq(Boolean.TRUE), eq(triggerType), eq(time), eq(mockAbnormalLog.getId()));
    }

    @Test
    @DisplayName("sendZoneSafety 성공 - abnormalLogOption 제공될 때, 해당 로그 사용")
    void sendZoneSafety_success_abnormalLogProvided() throws ExecutionException, InterruptedException, FirebaseMessagingException {
        // Arrange
        String zoneId = "zone1";
        Integer dangerLevel = 3; // Not used if abnormalLogOption provided for findLatest...
        TriggerType triggerType = TriggerType.MANUAL;
        LocalDateTime time = LocalDateTime.now();
        AbnormalLog providedAbnormalLog = new AbnormalLog();
        providedAbnormalLog.setId(99L);
        providedAbnormalLog.setTargetId("sensor99");

        Sensor providedSensor = new Sensor();
        providedSensor.setSensorType(SensorType.temp);

        Worker workerInZone = new Worker();
        workerInZone.setWorkerId("workerInZoneOnly");
        workerInZone.setFcmToken("tokenOnly");
        ZoneHist zoneHist = new ZoneHist();
        zoneHist.setWorker(workerInZone);
        List<ZoneHist> zoneHists = List.of(zoneHist);


        when(zoneRepoService.findById(zoneId)).thenReturn(mockZone);
        // findLatestSensorLogInZoneWithDangerLevel should NOT be called
        when(sensorRepoService.getSensorById(providedAbnormalLog.getTargetId())).thenReturn(providedSensor);
        when(zoneHistoryRepoService.getCurrentWorkersByZoneId(zoneId)).thenReturn(zoneHists);
        when(fcmPushService.sendMessage(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("messageId"));

        // Act
        fcmService.sendZoneSafety(zoneId, dangerLevel, triggerType, time, providedAbnormalLog);

        // Assert
        verify(abnormalLogRepoService, never()).findLatestSensorLogInZoneWithDangerLevel(any(), any(), anyInt());
        verify(sensorRepoService).getSensorById(providedAbnormalLog.getTargetId());

        String expectedBody = "Test Zone에 있는 작업자들은 temp 센서의 수치가 높으므로 주의하세요.";
        verify(fcmPushService).sendMessage(eq(workerInZone.getFcmToken()), anyString(), eq(expectedBody));

        verify(notifyLogService).saveNotifyLogFromFCM(
                eq(workerInZone.getWorkerId()),
                eq(Boolean.TRUE),
                eq(triggerType),
                eq(time),
                eq(providedAbnormalLog.getId())
        );
    }

    @Test
    @DisplayName("sendZoneSafety - 작업자가 없는 Zone일 경우 FCM 전송 및 NotifyLog 저장 안 함")
    void sendZoneSafety_noWorkersInZone() throws FirebaseMessagingException {
        // Arrange
        String zoneId = "zone1";
        Integer dangerLevel = 3;
        TriggerType triggerType = TriggerType.AUTOMATIC;
        LocalDateTime time = LocalDateTime.now();

        when(zoneRepoService.findById(zoneId)).thenReturn(mockZone);
        when(abnormalLogRepoService.findLatestSensorLogInZoneWithDangerLevel(TargetType.Sensor, mockZone, dangerLevel))
                .thenReturn(mockAbnormalLog); // This will still be called
        when(sensorRepoService.getSensorById(mockAbnormalLog.getTargetId())).thenReturn(mockSensor);
        when(zoneHistoryRepoService.getCurrentWorkersByZoneId(zoneId)).thenReturn(Collections.emptyList()); // No workers

        // Act
        fcmService.sendZoneSafety(zoneId, dangerLevel, triggerType, time, null);

        // Assert
        verify(fcmPushService, never()).sendMessage(anyString(), anyString(), anyString());
        verify(notifyLogService, never()).saveNotifyLogFromFCM(anyString(), anyBoolean(), any(), any(), anyLong());
    }

    @Nested
    @DisplayName("메세지만 작업자에게 전달할때 사용하는 메서드 테스트")
    class sendCustomMessage {
        @Test
        @DisplayName("정상 사용자, 정상 메세지를 전송할때")
        void bestSituation(){
            when(workerRepoService.findById(mockWorker.getWorkerId())).thenReturn(mockWorker);
            when(fcmPushService.sendMessage(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture("messageId"));
            when(notifyLogService.saveNotifyLogFromFCM(anyString(), anyBoolean(), any(), any(), anyLong())).thenReturn(NotifyLog.builder().build());
            fcmService.sendCustomMessage(mockWorker.getWorkerId(), "test Message");
            verify(fcmPushService).sendMessage(mockWorker.getFcmToken(), "[관리자 송신 알림]", "[관리자 메세지] test Message");
        }

        @Test
        @DisplayName("없는 사용자, 정상 메세지를 전송할때 -> 유저 유효성 검증에서 바로 확인 (NotFound)")
        void NotFoundUserSituation(){
            when(workerRepoService.findById("이상한 사용자")).thenThrow(new NotFoundException("없는 사용자 입니다."));
            assertThrows(NotFoundException.class,()->fcmService.sendCustomMessage("이상한 사용자", "test Message"));
        }

        @Test
        @DisplayName("메세지가 비어있는 경우 -> 빈 문자열일 경우 예외 처리(BadRequestException)")
        void EmptyMessageSituation(){
            when(workerRepoService.findById(mockWorker.getWorkerId())).thenReturn(mockWorker);
            assertThrows(BadRequestException.class,()->fcmService.sendCustomMessage(mockWorker.getWorkerId(), null));
            verify(fcmPushService, never()).sendMessage(anyString(), anyString(), anyString());
            verify(notifyLogService, never()).saveNotifyLogFromFCM(anyString(), anyBoolean(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("메세지가 비어있는 경우2 -> 빈 문자열일 경우 예외 처리(BadRequestException)")
        void EmptyMessageSituation2(){
            when(workerRepoService.findById(mockWorker.getWorkerId())).thenReturn(mockWorker);
            assertThrows(BadRequestException.class,()->fcmService.sendCustomMessage(mockWorker.getWorkerId(), ""));
            verify(fcmPushService, never()).sendMessage(anyString(), anyString(), anyString());
            verify(notifyLogService, never()).saveNotifyLogFromFCM(anyString(), anyBoolean(), any(), any(), anyLong());
        }
        @Test
        @DisplayName("토큰이 등록되지 않은 경우 -> BadRequestException")
        void NoTokenSituation(){
            Worker NoTokenWorker = mockWorker;
            NoTokenWorker.setFcmToken(null);
            when(workerRepoService.findById(mockWorker.getWorkerId())).thenReturn(NoTokenWorker);
            assertThrows(BadRequestException.class,()->fcmService.sendCustomMessage(mockWorker.getWorkerId(), "test Message"));
            verify(fcmPushService, never()).sendMessage(anyString(), anyString(), anyString());
            verify(notifyLogService, never()).saveNotifyLogFromFCM(anyString(), anyBoolean(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("토큰이 비어 있는 경우 -> BadRequestException")
        void NoTokenSituation2(){
            Worker NoTokenWorker = mockWorker;
            NoTokenWorker.setFcmToken("");
            when(workerRepoService.findById(mockWorker.getWorkerId())).thenReturn(NoTokenWorker);
            assertThrows(BadRequestException.class,()->fcmService.sendCustomMessage(mockWorker.getWorkerId(), "test Message"));
            verify(fcmPushService, never()).sendMessage(anyString(), anyString(), anyString());
            verify(notifyLogService, never()).saveNotifyLogFromFCM(anyString(), anyBoolean(), any(), any(), anyLong());
        }
    }
}