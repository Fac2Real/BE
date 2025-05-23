package com.factoreal.backend.domain.notifyLog.entity;

import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.wearable.entity.Wearable;
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
    @Column(name = "abnormal_id")
    private Long id; // 이상 감지 ID

    @Column(name = "notify_type", length = 50)
    private String notifyType; // 알림 유형

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt; // 알림 시각

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // 👉 FK = PK 매핑
    @JoinColumn(name = "abnormal_id")
    private AbnormalLog abnormalLog;  // FK = PK 매핑

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", referencedColumnName = "wearable_id", insertable = false, updatable = false)
    private Wearable recipient; // 수신자 ID
}
