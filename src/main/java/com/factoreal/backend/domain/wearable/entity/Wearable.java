package com.factoreal.backend.domain.wearable.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wearable_info")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
// 웨어러블 정보 Info
public class Wearable {

    @Id
    @Column(name = "wearable_id", length = 100, nullable = false, unique = true)
    private String wearableId; // 웨어러블 고유 ID

    @Column(name = "created_id", nullable = false)
    private LocalDateTime createdId; // 웨어러블 생성 시간
}