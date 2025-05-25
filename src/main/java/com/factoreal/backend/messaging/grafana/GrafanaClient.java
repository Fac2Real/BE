package com.factoreal.backend.messaging.grafana;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrafanaClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${grafana.url}")
    private String url;

    @Value("${grafana.api-key}")
    private String apiKey;

    private RestTemplate rest() {
        return restTemplateBuilder.rootUri(url).build();
    }

    public String createDashboard(String dashboardJson) {
        RestTemplate restTemplate = rest();

        // 1) 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2) HttpEntity에 payload + headers 담기
        HttpEntity<String> entity = new HttpEntity<>(dashboardJson, headers);

        // 3) POST 요청
        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                "/api/dashboards/db",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );

        Map<String, Object> body = res.getBody();
        if (body == null || body.get("uid") == null) {
            log.error("Grafana API 대시보드 생성 실패: {}", body);
            throw new IllegalArgumentException("Cannot create dashboard");
        }

        return body.get("uid").toString();
    }

}
