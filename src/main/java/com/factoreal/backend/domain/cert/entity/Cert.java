package com.factoreal.backend.domain.cert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cert_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cert {

    /** 인증서 ID (PK) */
    @Id
    @Column(name = "cert_id", length = 100, nullable = false)
    private String certId;

    /** 인증 등급 (예: A/B/C …) */
    @Column(name = "grade", length = 100)
    private String grade;

    /** 발급 일시 */
    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    /** 갱신(수정) 일시 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 만료 예정 일시 */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    /** 평가 점수 */
    @Column(name = "cert_score")
    private Integer certScore;

    /** 평가 내용 요약
     *  (ERD 컬럼명이 오타(discription)라면 그대로 매핑) */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 인증서 상태 (유효/만료/취소 등) */
    @Column(name = "status", length = 100)
    private String status;
}
