package com.factoreal.backend.domain.equip.entity;

import com.factoreal.backend.domain.zone.entity.Zone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "equip_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 설비정보 Table
public class Equip {
    @Id
    @Column(name = "equip_id", length = 100, nullable = false, unique = true)
    private String equipId;  // 설비ID

    @Column(name = "equip_name", length =  255 , nullable = false)
    private String equipName; // 설비명
    
    // Foreign Key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", referencedColumnName = "zone_id", nullable = false)
    private Zone zone; // 공간 고유 ID
}
