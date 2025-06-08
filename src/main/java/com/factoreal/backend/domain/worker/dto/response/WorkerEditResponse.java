package com.factoreal.backend.domain.worker.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 수정 화면에 내려줄 응답 */
@Builder
@Getter
public class WorkerEditResponse {
    private String workerId;
    private String name;
    private String phoneNumber;
    private String email;
    private List<String> zoneNames;      // 출입 권한
    private boolean manager;             // 담당자 여부
    private List<String> managedZones;   // 담당 공간 리스트
}