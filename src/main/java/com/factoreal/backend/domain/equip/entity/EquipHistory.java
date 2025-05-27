package com.factoreal.backend.domain.equip.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "equip_hist")
public class EquipHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 설비 기록 PK

    @JoinColumn(name = "equip_id", referencedColumnName = "equip_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Equip equip;

    @Column(name = "date", length = 255, nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EquipHistoryType type;


}
