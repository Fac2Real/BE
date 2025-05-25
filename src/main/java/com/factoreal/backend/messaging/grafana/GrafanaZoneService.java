package com.factoreal.backend.messaging.grafana;

import com.factoreal.backend.domain.sensor.dao.SensorRepository;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GrafanaZoneService {
    private final SensorRepository sensorRepository;
    private final DashboardFactory dashboardFactory;
    private final GrafanaClient grafanaClient;

    @Value("${grafana.url}")
    private String grafanaUrl;

    @Value("${grafana.org-id}")
    private int orgId;

    @Value("${grafana.datasource-uid}")
    private String datasourceUid;

    // 30 분 임계
    private static final Duration THRESHOLD = Duration.ofMinutes(30);

    /**
     * zoneId용 대시보드를 1개 생성하고,
     * (sensorName → panel별 iframe URL) 맵을 반환
     */
    public List<GrafanaSensorResponseDto> createDashboardUrls(String zoneId) throws JsonProcessingException {
        // 1) 센서 목록 조회
        List<Sensor> sensors = sensorRepository.findByZone_ZoneId(zoneId);
        if (sensors.isEmpty()) {
            throw new IllegalStateException("No sensors for zone: " + zoneId);
        }

        // 2) 기존 대시보드 검색 : TODO

        // 3) 대시보드 JSON (센서 패널 다 포함)
        String json = dashboardFactory.build(zoneId, sensors, datasourceUid);

        // 4) Grafana에 대시보드 1개 생성
        String dashboardUid = grafanaClient.createDashboard(json);

        // 5) 센서 이름 → iframe URL 매핑 (panelId = 인덱스+1)
        List<GrafanaSensorResponseDto> responses = new ArrayList<>();
        for (int i = 0; i < sensors.size(); i++) {
            String sensorId = sensors.get(i).getSensorId();
            String sensorType = sensors.get(i).getSensorType().toString().toUpperCase();
            int panelId = i + 1;

            String iframeUrl = String.format(
                    "%s/d-solo/%s/%s?orgId=%d&panelId=%d&kiosk=tv&from=now-1h&to=now",
                    grafanaUrl, dashboardUid, sensorId, orgId, panelId
            );
            responses.add(new GrafanaSensorResponseDto(sensorId, sensorType, iframeUrl));
        }
        return responses;
    }
}
