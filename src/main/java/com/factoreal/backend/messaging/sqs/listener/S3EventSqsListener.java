package com.factoreal.backend.messaging.sqs.listener;

import com.factoreal.backend.messaging.sqs.processor.EquipPredictProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor // 이 위치로 옮기고 import 경로도 바꿔주세요
public class S3EventSqsListener {

    private final EquipPredictProcessor equipPredictProcessor;
    private final ObjectMapper objectMapper;

    // application-*.yml 에서 설정된 큐 URL 을 주입
    @Value("${aws.sqs.queue-url}")
    private String queueUrl;


    // ① 큐 이름 또는 Queue URL을 value 작성
    //   (Queue URL 을 쓰면 renaming 걱정 없음)
    @SqsListener("${aws.sqs.queue-url}")
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
                    // key에서 equip_id와 zone_id 추출
                    String zoneId = null, equipId = null;
                    for(String segment : key.split("/")) {
                        if (segment.startsWith("zone_id=")) {
                            zoneId = segment.substring("zone_id=".length());
                        } else if (segment.startsWith("equip_id=")) {
                            equipId = segment.substring("equip_id=".length());
                        }
                    }

                    if (zoneId != null && equipId != null) {

                        log.info("👀 서비스 호출 예정 (zoneId={}, equipId={})", zoneId, equipId);
                        equipPredictProcessor.equipPredProcess(zoneId, equipId);
                    } else {
                        log.warn("⚠️ zone_id 또는 equip_id를 추출하지 못했습니다: {}", key);
                    }
                }
            }
        } catch (Exception e) {
            log.error("S3 이벤트 파싱 실패", e);
            // 필요 시 재시도 로직 or 예외 던지기
        }


    }
}

