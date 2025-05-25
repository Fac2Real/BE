package com.factoreal.backend.domain.zone.entity;

import com.factoreal.backend.domain.worker.entity.Worker;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "zone_hist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// zone_hist ENTITY
public class ZoneHist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;  // ID

    @Column(name = "start_time")
    private LocalDateTime startTime; // 공간 입장 시간

    @Column(name = "end_time")
    private LocalDateTime endTime; // 공간 퇴장 시간

    @Column(name = "exist_flag")
    private Integer existFlag; // 공간 존재 여부

    // FK: zone_id → zone_info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", referencedColumnName = "zone_id")
    private Zone zone; // 공간 고유 ID

    // FK: worker_id → worker_info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", referencedColumnName = "worker_id")
    private Worker worker; // 작업자 고유 ID

}
