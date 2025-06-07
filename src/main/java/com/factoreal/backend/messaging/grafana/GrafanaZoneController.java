package com.factoreal.backend.messaging.grafana;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/grafana-zone")
public class GrafanaZoneController {
    private final GrafanaZoneService grafanaZoneService;

    @GetMapping("/{zoneId}/dashboards")
    public List<GrafanaSensorResponseDto> getDashboards(@PathVariable String zoneId) throws JsonProcessingException {
        log.info("Zone ID: {}에 대한 환경 리포트 요청", zoneId);
        return grafanaZoneService.createDashboardUrls(zoneId);
    }
}
