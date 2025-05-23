package com.factoreal.backend.domain.sensor.entity;

import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 센서 정보 Entity
@Entity
@Table(name = "sensor_info")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Sensor {

    @Id
    @Column(name = "sensor_id", length = 100, nullable = false, unique = true) // 센서ID
    private String sensorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type") // 센서종류
    private SensorType sensorType;

    @Column(name = "val_unit", length = 10)
    private String valUnit;

    @Column(name = "sensor_thres")
    private Double sensorThres;  // 임계치

    @Column(name = "allow_val")
    private Double allowVal; // 허용치

    @Column(name = "created_at")
    private LocalDateTime createdAt; // 센서생성시간

    @Column(name = "iszone")
    private Integer isZone;  // 1: 공간용 센서 / 0: 설비용 센서

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone; // 공간 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equip_id", nullable = false)
    private Equip equip; // 설비 고유 ID

}