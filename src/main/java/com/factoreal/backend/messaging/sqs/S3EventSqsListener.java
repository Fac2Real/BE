package com.factoreal.backend.messaging.sqs;

import com.factoreal.backend.domain.equip.application.EquipMaintenanceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor // 이 위치로 옮기고 import 경로도 바꿔주세요
public class S3EventSqsListener {

    private final EquipMaintenanceService equipMaintenanceService;
    private final ObjectMapper objectMapper;

    // ① 큐 이름 또는 Queue URL을 value 작성
    //   (Queue URL 을 쓰면 renaming 걱정 없음)
    @SqsListener(value = "https://sqs.ap-northeast-2.amazonaws.com/853660505909/S3NewJsonEventAlert")
    public void handleMessage(String rawMessage) {

        log.info("📥 SQS 메시지 수신: {}", rawMessage);
        try {

            JsonNode root = objectMapper.readTree(rawMessage);

            // 1) Records 배열이 있으면 그쪽, 없으면 루트 자체를 단일 이벤트로 처리
            List<JsonNode> events = new ArrayList<>();
            JsonNode recordsNode = root.path("Records");
            if (recordsNode.isArray() && recordsNode.size() > 0) {
                recordsNode.forEach(events::add);
            } else {
                events.add(root);
            }

            // 2) 이벤트별 공통 로직
            for (JsonNode rec : events) {
                String eventName = rec.path("eventName").asText("");
                if (!eventName.startsWith("ObjectCreated")) {
                    continue;
                }

                JsonNode s3 = rec.path("s3");
                String bucket = s3.path("bucket").path("name").asText("");
                String keyEnc = s3.path("object").path("key").asText("");
                String key    = URLDecoder.decode(keyEnc, StandardCharsets.UTF_8);

                log.info("➡️ bucket={}, key={}", bucket, key);

                if (key.startsWith("EQUIPMENT/") && key.endsWith(".json")) {
                    log.info("👀 서비스 호출 예정");
//                    equipMaintenanceService.fetchAndProcessMaintenancePredictions();
                }
            }
        } catch (Exception e) {
            log.error("S3 이벤트 파싱 실패", e);
            // 필요 시 재시도 로직 or 예외 던지기
        }


    }
}

