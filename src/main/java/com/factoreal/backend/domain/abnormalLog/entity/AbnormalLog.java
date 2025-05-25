package com.factoreal.backend.domain.abnormalLog.entity;

import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "abn_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbnormalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 50)
    private TargetType targetType; // 구분 분류 :  Sensor(공간- 012 rule-base), Worker, Equip(설비-머신러닝) 구분

    @Column(name = "target_id", length = 100)
    private String targetId; // 고유 ID : 센서ID, WorkerID, EquipID

    @Column(name = "abnormal_type", length = 100)
    private String abnormalType; // 이상 유형 : (예: 심박수 위험, 온도 초과 위험, 진동 주의 등)
                                 // 이상은 위험과 주의로 구분이 애매하므로 명확한 표현 필요

    @Column(name = "abn_val")
    private Double abnVal; // 이상치 값

    @Column(name = "danger_level")
    private Integer dangerLevel;

    @Column(name = "detected_at")
    @CreatedDate
    private LocalDateTime detectedAt; // 이상 감지 시간

    // FK: zone_id → zone_info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", referencedColumnName = "zone_id")
    private Zone zone; // 공간 고유 ID

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    public AbnormalLogResponse fromEntity() {
        return AbnormalLogResponse.builder()
            .id(this.getId())
            .targetType(this.getTargetType())
            .targetId(this.getTargetId())
            .abnormalType(this.getAbnormalType())
            .abnVal(this.getAbnVal())
            .detectedAt(this.getDetectedAt())
            .zoneId(this.getZone().getZoneId())
            .zoneName(this.getZone().getZoneName())
            .build();
    }
}
