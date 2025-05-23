package com.factoreal.backend.domain.worker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "worker_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Worker {

    @Id
    @Column(name = "worker_id", length = 100, nullable = false)
    private String workerId;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber; // 작업자 번호 (국가번호+전화번호의 조합) ex) +8201012345678

    @Column(name = "email", length = 100)
    private String email;
}