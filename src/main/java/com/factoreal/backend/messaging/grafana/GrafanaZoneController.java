package com.factoreal.backend.messaging.grafana;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/grafana-zone")
public class GrafanaZoneController {
    private final GrafanaZoneService grafanaZoneService;

    @GetMapping("/{zoneId}/dashboards")
    public List<GrafanaSensorResponseDto> getDashboards(@PathVariable String zoneId) throws JsonProcessingException {
        return grafanaZoneService.createDashboardUrls(zoneId);
    }
}
