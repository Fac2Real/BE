package com.factoreal.backend.domain.equip.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "equip_hist")
public class EquipHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 설비 기록 PK

    @JoinColumn(name = "equip_id", referencedColumnName = "equip_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Equip equip; // 기록되는 설비의 id

    @Column(name = "date", nullable = false)
    private LocalDate date; // 교체일자 (YYYY-MM-DD)

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EquipHistoryType type; // 기록의 종류 (UPDATE, ACCIDENT)
}
