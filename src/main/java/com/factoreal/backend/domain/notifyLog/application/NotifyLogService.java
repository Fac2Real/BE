package com.factoreal.backend.domain.notifyLog.application;

import com.factoreal.backend.domain.notifyLog.dto.NotifyType;
import com.factoreal.backend.domain.notifyLog.dto.TriggerType;
import com.factoreal.backend.domain.notifyLog.entity.NotifyLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotifyLogService {
    private final NotifyLogRepoService notifyLogRepoService;

    /**
     * Websocket Notify 기록을 저장하기 위함 -> 화면에서는 사용 x
     * 사용위치 -> WearableEventProcessor, SensorEventProcessor 2군데서 사용중, 실패를 추적하기 위해 성공시 호출, 실패시 호출
     *
     * @param webSocketTopic
     * @param success
     * @param triggerType
     * @param triggerTime
     * @param abnormalLogId
     * @return
     */
    public NotifyLog saveNotifyLogFromWebsocket(
            String webSocketTopic,
            Boolean success,
            TriggerType triggerType,
            LocalDateTime triggerTime,
            Long abnormalLogId
    ) {
        NotifyLog notifyLog = NotifyLog.builder()
                .notifyType(NotifyType.WebSocket)
                .target(webSocketTopic) // websocket topic 경로
                .status(success) // 전송 성공 여부
                .triggerType(triggerType) // 자동발송/수동 발송 여부
                .notifiedAt(triggerTime) // 알림 시각
                .abnormalLogId(abnormalLogId) // abnormalId
                .build();

        return notifyLogRepoService.saveNotifyLog(notifyLog);
    }

    /**
     * 아직 어디서 사용할지 모르겠음
     *
     * @param slackUrl
     * @param success
     * @param triggerType
     * @param triggerTime
     * @param abnormalLogId
     * @return
     */
    public NotifyLog saveNotifyLogFromSlack(
            String slackUrl,
            Boolean success,
            TriggerType triggerType,
            LocalDateTime triggerTime,
            Long abnormalLogId
    ) {
        NotifyLog notifyLog = NotifyLog.builder()
                .notifyType(NotifyType.Slack)
                .target(slackUrl) // slack 채널 구분
                .status(success) // 전송 성공 여부
                .triggerType(triggerType) // 자동발송/수동 발송 여부
                .notifiedAt(triggerTime) // 알림 시각
                .abnormalLogId(abnormalLogId) // abnormalId
                .build();
        return notifyLogRepoService.saveNotifyLog(notifyLog);
    }

    /**
     * 작업자를 호출한 경우 기록하기 위한 함수 -> Environment인 경우와 수동호출에 해당됨.
     *
     * @param workerId
     * @param success
     * @param triggerType
     * @param triggerTime
     * @param abnormalLogId
     * @return
     */
    public NotifyLog saveNotifyLogFromFCM(
            String workerId,
            Boolean success,
            TriggerType triggerType,
            LocalDateTime triggerTime,
            Long abnormalLogId
    ) {
        NotifyLog notifyLog = NotifyLog.builder()
                .notifyType(NotifyType.AppPush)
                .target(workerId) // fcm workerId
                .status(success) // 전송 성공 여부
                .triggerType(triggerType) // 자동발송/수동 발송 여부
                .notifiedAt(triggerTime) // 알림 시각
                .abnormalLogId(abnormalLogId) // abnormalId 수동 발송이여서 대부분 null
                .build();
        return notifyLogRepoService.saveNotifyLog(notifyLog);
    }
}

