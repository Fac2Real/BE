package com.factoreal.backend.domain.wearable.entity;

import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.entity.Zone;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wearable_hist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// wearable_hist ENTITY
public class WearableHist {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;  // ID

    @Column(name = "start_date")
    private LocalDateTime startDate; // 웨어러블 착용 시간

    @Column(name = "end_date")
    private LocalDateTime endDate; // 웨어러블 반납시간

    @Column(name = "use_flag")
    private Integer useFlag; // 현재 사용여부

    // FK: wearable_id → wearable_info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wearable_id", referencedColumnName = "wearable_id")
    private Wearable wearable; // 웨어러블 고유 ID

    // FK: worker_id → worker_info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", referencedColumnName = "worker_id")
    private Worker worker; // 작업자 고유 ID

    // FK: zone_id → zone_info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", referencedColumnName = "zone_id")
    private Zone zone; // 공간 고유 ID
}
