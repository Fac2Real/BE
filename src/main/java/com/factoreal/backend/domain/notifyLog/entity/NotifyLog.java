package com.factoreal.backend.domain.notifyLog.entity;

import com.factoreal.backend.domain.notifyLog.dto.NotifyType;
import com.factoreal.backend.domain.notifyLog.dto.TriggerType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notify_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotifyLog {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 알람 ID

    @Enumerated(EnumType.STRING)
    @Column(name = "notify_type", length = 50)
    private NotifyType notifyType; // 알림 유형 AppPush, WebSocket, Slack

    @Column(name = "target", length = 200)
    private String target; // FCM -> workerId | Websocket -> 토픽 경로 | Slack -> 슬랙 채널 url

    @Column(name = "status")
    private Boolean status; // 전송 성공 여부

    @Column(name = "trigger_type", length = 50)
    private TriggerType triggerType; // 자동 발송, 수동 발송

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt; // 알림 시각

    @Column(name = "abnormal_id")
    private Long abnormalLogId;  // FK = PK 매핑 -> 특정 Dto에서 abnormalId만 확인할 수 있어서 JPA 레벨에서의 연결을 제거
}
