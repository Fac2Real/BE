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

    @Column(name = "accident_date", nullable = false)
    private LocalDate accidentDate; // 예상 점검일자

    @Column(name = "check_date")
    private LocalDate checkDate; // 실제 점검일자
}
