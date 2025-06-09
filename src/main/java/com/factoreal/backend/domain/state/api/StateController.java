package com.factoreal.backend.domain.state.api;

import com.factoreal.backend.domain.state.service.StateService;
import com.factoreal.backend.messaging.common.dto.ZoneDangerDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/state")
@RequiredArgsConstructor
public class StateController {
    private final StateService service;

    @GetMapping("/zones")
    @Operation(summary = "공간 상태 조회", description = "등록된 모든 공간 상태 정보를 조회합니다.")
    public List<ZoneDangerDto> getAllZoneStates() {
        return service.getAllZoneStates();
    }
}
