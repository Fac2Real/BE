package com.factoreal.backend.domain.notifyLog.application;

import com.factoreal.backend.domain.notifyLog.dto.NotifyType;
import com.factoreal.backend.domain.notifyLog.dto.TriggerType;
import com.factoreal.backend.domain.notifyLog.entity.NotifyLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // Mockito를 사용해 단위 테스트를 실행하기 위한 설정
class NotifyLogServiceTest {

    @Mock // NotifyLogRepoService를 모킹하여 의존성을 제거
    private NotifyLogRepoService notifyLogRepoService;

    @InjectMocks // 실제 테스트할 대상인 NotifyLogService에 모킹된 notifyLogRepoService를 주입
    private NotifyLogService notifyLogService;

    @Test
    @DisplayName("WebSocket 로그 저장 테스트 - 정상 케이스")
    void saveNotifyLogFromWebsocket_shouldSaveCorrectLog() {
        // 테스트 목적:
        // WebSocket을 통해 NotifyLog를 저장할 때, 올바른 필드로 NotifyLog 객체가 생성되고,
        // Repository에 정상적으로 저장 요청이 수행되는지 검증

        // Arrange (입력 및 기대값 준비)
        String webSocketTopic = "/topic/test";
        Boolean success = true;
        TriggerType triggerType = TriggerType.AUTOMATIC;
        LocalDateTime triggerTime = LocalDateTime.now();
        Long abnormalLogId = 1L;

        NotifyLog expectedSavedLog = NotifyLog.builder()
                .id(100L)
                .notifyType(NotifyType.WebSocket)
                .target(webSocketTopic)
                .status(success)
                .triggerType(triggerType)
                .notifiedAt(triggerTime)
                .abnormalLogId(abnormalLogId)
                .build();

        // Mock구현에 상관없이 saveNotifyLog에 대해서 expectedSaveLog가 반환된다고 가정.
        when(notifyLogRepoService.saveNotifyLog(any(NotifyLog.class))).thenReturn(expectedSavedLog);

        // Act (테스트 대상 메서드 호출)
        NotifyLog actualSavedLog = notifyLogService.saveNotifyLogFromWebsocket(
                webSocketTopic, success, triggerType, triggerTime, abnormalLogId);

        // Assert (결과 검증)
        assertNotNull(actualSavedLog);
        assertEquals(expectedSavedLog, actualSavedLog);

        // 실제 저장된 객체의 필드가 기대값과 일치하는지 확인
        ArgumentCaptor<NotifyLog> notifyLogCaptor = ArgumentCaptor.forClass(NotifyLog.class);
        verify(notifyLogRepoService, times(1)).saveNotifyLog(notifyLogCaptor.capture());

        NotifyLog capturedLog = notifyLogCaptor.getValue();
        assertEquals(NotifyType.WebSocket, capturedLog.getNotifyType());
        assertEquals(webSocketTopic, capturedLog.getTarget());
        assertEquals(success, capturedLog.getStatus());
        assertEquals(triggerType, capturedLog.getTriggerType());
        assertEquals(triggerTime, capturedLog.getNotifiedAt());
        assertEquals(abnormalLogId, capturedLog.getAbnormalLogId());
    }

    @Test
    @DisplayName("WebSocket 로그 저장 테스트 - 실패 상태 케이스")
    void saveNotifyLogFromWebsocket_shouldHandleFailureStatus() {
        // 테스트 목적:
        // WebSocket 알림이 실패한 경우에도 NotifyLog가 정상적으로 저장되는지 확인

        // Arrange
        String webSocketTopic = "/topic/failure";
        Boolean success = false;
        TriggerType triggerType = TriggerType.MANUAL;
        LocalDateTime triggerTime = LocalDateTime.now().minusHours(1);
        Long abnormalLogId = 2L;

        NotifyLog expectedSavedLog = NotifyLog.builder().id(101L).status(false).build();
        when(notifyLogRepoService.saveNotifyLog(any(NotifyLog.class))).thenReturn(expectedSavedLog);

        // Act
        NotifyLog actualSavedLog = notifyLogService.saveNotifyLogFromWebsocket(
                webSocketTopic, success, triggerType, triggerTime, abnormalLogId);

        // Assert
        assertNotNull(actualSavedLog);
        assertEquals(expectedSavedLog, actualSavedLog);
        assertEquals(false, actualSavedLog.getStatus());

        ArgumentCaptor<NotifyLog> notifyLogCaptor = ArgumentCaptor.forClass(NotifyLog.class);
        verify(notifyLogRepoService, times(1)).saveNotifyLog(notifyLogCaptor.capture());

        NotifyLog capturedLog = notifyLogCaptor.getValue();
        assertEquals(NotifyType.WebSocket, capturedLog.getNotifyType());
        assertEquals(webSocketTopic, capturedLog.getTarget());
        assertEquals(success, capturedLog.getStatus());
        assertEquals(triggerType, capturedLog.getTriggerType());
        assertEquals(triggerTime, capturedLog.getNotifiedAt());
        assertEquals(abnormalLogId, capturedLog.getAbnormalLogId());
    }

    @Test
    @DisplayName("Slack 로그 저장 테스트 - 정상 케이스")
    void saveNotifyLogFromSlack_shouldSaveCorrectLog() {
        // 테스트 목적:
        // Slack 알림 로그가 NotifyLog로 올바르게 저장되는지 확인

        // Arrange
        String slackUrl = "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX";
        Boolean success = true;
        TriggerType triggerType = TriggerType.AUTOMATIC;
        LocalDateTime triggerTime = LocalDateTime.now();
        Long abnormalLogId = 3L;

        NotifyLog expectedSavedLog = NotifyLog.builder()
                .id(200L)
                .notifyType(NotifyType.Slack)
                .target(slackUrl)
                .status(success)
                .triggerType(triggerType)
                .notifiedAt(triggerTime)
                .abnormalLogId(abnormalLogId)
                .build();

        when(notifyLogRepoService.saveNotifyLog(any(NotifyLog.class))).thenReturn(expectedSavedLog);

        // Act
        NotifyLog actualSavedLog = notifyLogService.saveNotifyLogFromSlack(
                slackUrl, success, triggerType, triggerTime, abnormalLogId);

        // Assert
        assertNotNull(actualSavedLog);
        assertEquals(expectedSavedLog, actualSavedLog);

        ArgumentCaptor<NotifyLog> notifyLogCaptor = ArgumentCaptor.forClass(NotifyLog.class);
        verify(notifyLogRepoService, times(1)).saveNotifyLog(notifyLogCaptor.capture());

        NotifyLog capturedLog = notifyLogCaptor.getValue();
        assertEquals(NotifyType.Slack, capturedLog.getNotifyType());
        assertEquals(slackUrl, capturedLog.getTarget());
        assertEquals(success, capturedLog.getStatus());
        assertEquals(triggerType, capturedLog.getTriggerType());
        assertEquals(triggerTime, capturedLog.getNotifiedAt());
        assertEquals(abnormalLogId, capturedLog.getAbnormalLogId());
    }

    @Test
    @DisplayName("FCM 로그 저장 테스트 - abnormalLogId가 null인 경우")
    void saveNotifyLogFromFCM_shouldSaveCorrectLog() {
        // 테스트 목적:
        // FCM 알림 로그 저장 시 abnormalLogId가 null인 경우에도 정상 동작하는지 확인

        // Arrange
        String workerId = "worker-fcm-token-123";
        Boolean success = true;
        TriggerType triggerType = TriggerType.MANUAL;
        LocalDateTime triggerTime = LocalDateTime.now();
        Long abnormalLogId = null;

        NotifyLog expectedSavedLog = NotifyLog.builder()
                .id(300L)
                .notifyType(NotifyType.AppPush)
                .target(workerId)
                .status(success)
                .triggerType(triggerType)
                .notifiedAt(triggerTime)
                .abnormalLogId(abnormalLogId)
                .build();

        when(notifyLogRepoService.saveNotifyLog(any(NotifyLog.class))).thenReturn(expectedSavedLog);

        // Act
        NotifyLog actualSavedLog = notifyLogService.saveNotifyLogFromFCM(
                workerId, success, triggerType, triggerTime, abnormalLogId);

        // Assert
        assertNotNull(actualSavedLog);
        assertEquals(expectedSavedLog, actualSavedLog);

        ArgumentCaptor<NotifyLog> notifyLogCaptor = ArgumentCaptor.forClass(NotifyLog.class);
        verify(notifyLogRepoService, times(1)).saveNotifyLog(notifyLogCaptor.capture());

        NotifyLog capturedLog = notifyLogCaptor.getValue();
        assertEquals(NotifyType.AppPush, capturedLog.getNotifyType());
        assertEquals(workerId, capturedLog.getTarget());
        assertEquals(success, capturedLog.getStatus());
        assertEquals(triggerType, capturedLog.getTriggerType());
        assertEquals(triggerTime, capturedLog.getNotifiedAt());
        assertNull(capturedLog.getAbnormalLogId()); // null인 경우 검증
    }

    @Test
    @DisplayName("FCM 로그 저장 테스트 - abnormalLogId가 존재하는 경우")
    void saveNotifyLogFromFCM_shouldHandleNonNullAbnormalLogId() {
        // 테스트 목적:
        // abnormalLogId가 있는 FCM 알림 로그 저장이 올바르게 동작하는지 확인

        // Arrange
        String workerId = "worker-fcm-token-456";
        Boolean success = false;
        TriggerType triggerType = TriggerType.AUTOMATIC;
        LocalDateTime triggerTime = LocalDateTime.now().plusDays(1);
        Long abnormalLogId = 4L;

        NotifyLog expectedSavedLog = NotifyLog.builder().id(301L).abnormalLogId(abnormalLogId).build();
        when(notifyLogRepoService.saveNotifyLog(any(NotifyLog.class))).thenReturn(expectedSavedLog);

        // Act
        NotifyLog actualSavedLog = notifyLogService.saveNotifyLogFromFCM(
                workerId, success, triggerType, triggerTime, abnormalLogId);

        // Assert
        assertNotNull(actualSavedLog);
        assertEquals(expectedSavedLog, actualSavedLog);
        assertEquals(abnormalLogId, actualSavedLog.getAbnormalLogId());

        ArgumentCaptor<NotifyLog> notifyLogCaptor = ArgumentCaptor.forClass(NotifyLog.class);
        verify(notifyLogRepoService, times(1)).saveNotifyLog(notifyLogCaptor.capture());

        NotifyLog capturedLog = notifyLogCaptor.getValue();
        assertEquals(NotifyType.AppPush, capturedLog.getNotifyType());
        assertEquals(workerId, capturedLog.getTarget());
        assertEquals(success, capturedLog.getStatus());
        assertEquals(triggerType, capturedLog.getTriggerType());
        assertEquals(triggerTime, capturedLog.getNotifiedAt());
        assertEquals(abnormalLogId, capturedLog.getAbnormalLogId());
    }
}
