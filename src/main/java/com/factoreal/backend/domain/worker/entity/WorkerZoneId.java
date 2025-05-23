package com.factoreal.backend.domain.worker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WorkerZoneId implements Serializable {

    @Column(name = "worker_id", length = 100)
    private String workerId;

    @Column(name = "zone_id", length = 100)
    private String zoneId;
}
