package com.factoreal.backend.domain.controlLog.entity;

import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.zone.entity.Zone;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "control_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// control_log ENTITY
public class ControlLog {

    @Id
    @Column(name = "abnormal_id")
    private Long id;

    @Column(name = "control_type", length = 50)
    private String controlType;

    @Column(name = "control_val")
    private Integer controlVal;

    @Column(name = "control_stat")
    private Integer controlStat;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    // AbnormalLog 연관 관계 (PK이자 FK)
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "abnormal_id")
    private AbnormalLog abnormalLog;

    // Zone 연관 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", referencedColumnName = "zone_id")
    private Zone zone;


}
