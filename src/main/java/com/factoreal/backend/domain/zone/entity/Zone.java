package com.factoreal.backend.domain.zone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "zone_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 공간정보 매핑 Table
public class Zone {

    @Id
    @Column(name = "zone_id", length = 100, nullable = false, unique = true)
    private String zoneId; // 공간ID

    @Column(name = "zone_name", length = 255 , nullable = false)
    private String zoneName; // 공간명
    
}