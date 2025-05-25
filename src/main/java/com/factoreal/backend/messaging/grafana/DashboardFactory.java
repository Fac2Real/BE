package com.factoreal.backend.messaging.grafana;

import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DashboardFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 센서별 패널을 포함한 대시보드 JSON 반환
     */
    public String build(String zoneId,
                        List<Sensor> sensors,
                        String datasourceUid) throws JsonProcessingException {

        // 루트 JSON 객체
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode dash = root.putObject("dashboard");

        // 대시보드 메타
        dash.put("uid", UUID.randomUUID().toString().substring(0, 12));
        dash.put("title", zoneId);
        dash.put("timezone", "browser");

        // 자동 데이터 갱신: 최근 15분의 시계열을 2초마다 갱신합니다
        dash.set("time", MAPPER.readTree("{\"from\":\"now-15m\",\"to\":\"now\"}"));
        dash.put("refresh", "2s");

        // panels 배열
        ArrayNode panels = dash.putArray("panels");

        // 센서별 패널 생성
        for (int i = 0; i < sensors.size(); i++) {
            String sensorId = sensors.get(i).getSensorId();

            // 패널 객체
            ObjectNode panel = MAPPER.createObjectNode();
            panel.put("id", i + 1);
            panel.put("type", "timeseries");
            panel.put("title", sensorId + " (Last 15min)");
            panel.set("gridPos", MAPPER.readTree(
                    String.format("{\"x\": %d, \"y\": 0, \"w\": 12, \"h\": 8}",
                            (i % 2) * 12)  // 좌우 2열 배치
            ));
            panel.put("datasource", datasourceUid);

            // targets 배열
            ArrayNode targets = panel.putArray("targets");
            ObjectNode tgt = targets.addObject();
            tgt.put("refId", "A");
            tgt.put("measurement", "ENVIRONMENT");
            tgt.put("policy", "default");
            tgt.put("resultFormat", "time_series");
            tgt.put("orderByTime", "ASC");

            // SELECT clause: raw field(val)
            ArrayNode select = tgt.putArray("select");
            ArrayNode selectClause = select.addArray();

            ObjectNode fieldStep = MAPPER.createObjectNode();
            fieldStep.put("type", "field");
            fieldStep.putArray("params").add("val");
            selectClause.add(fieldStep);

            // Tag filter: sensor = current sensorId
            ArrayNode tags = tgt.putArray("tags");
            ObjectNode tagFilter = MAPPER.createObjectNode();
            tagFilter.put("key", "sensorId::field");
            tagFilter.put("operator", "=");
            tagFilter.put("value", sensorId);
            tags.add(tagFilter);

            tgt.put("datasource", datasourceUid);
            tgt.put("hide", false);

            panels.add(panel);
        }

        // 기타 옵션
        root.put("folderId", 0);
        root.put("overwrite", false);

        return MAPPER.writeValueAsString(root);
    }
}
