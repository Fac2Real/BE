package com.factoreal.backend.domain.worker.entity;

import com.factoreal.backend.domain.zone.entity.Zone;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "worker_zone")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerZone {

    @EmbeddedId
    private WorkerZoneId id;

    @Column(name = "manage_yn", nullable = false)
    private Boolean manageYn; // 공간담당자 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("workerId")
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("zoneId")
    @JoinColumn(name = "zone_id")
    private Zone zone;
}
