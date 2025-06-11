package com.factoreal.backend.messaging.slack.api;

import com.slack.api.Slack;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.webhook.WebhookPayloads;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class SlackEquipAlarmService {
    private static final String NEW_LINE = "\n";

    @Value("${webhook.slack.equip_url}")
    private String SLACK_WEBHOOK_EQUIP_URL;

    private final Slack slackClient;

    public SlackEquipAlarmService() {
        this.slackClient = Slack.getInstance();
    }

    // For testing purposes
    SlackEquipAlarmService(Slack slackClient) {
        this.slackClient = slackClient;
    }

    // 설비 점검 알림 전송
    public void sendEquipmentMaintenanceAlert(String equipmentName, String zoneName, LocalDate expectedMaintenanceDate, long daysUntilMaintenance) throws IOException {
        // Null checks
        if (equipmentName == null) {
            throw new IllegalArgumentException("Equipment name cannot be null");
        }
        if (zoneName == null) {
            throw new IllegalArgumentException("Zone name cannot be null");
        }
        if (expectedMaintenanceDate == null) {
            throw new IllegalArgumentException("Expected date cannot be null");
        }

        log.info("Sending Slack alert for equipment: {}, zone: {}, expected date: {}, days until: {}",
                equipmentName, zoneName, expectedMaintenanceDate, daysUntilMaintenance);
        log.info("Using webhook URL: {}", SLACK_WEBHOOK_EQUIP_URL);

        try {
            List<LayoutBlock> layoutBlocks = generateLayoutBlock(equipmentName, zoneName, expectedMaintenanceDate, daysUntilMaintenance);

            slackClient.send(SLACK_WEBHOOK_EQUIP_URL, WebhookPayloads
                    .payload(p -> p.blocks(layoutBlocks))
            );
            log.info("Successfully sent Slack alert");
        } catch (Exception e) {
            log.error("Failed to send Slack alert: {}", e.getMessage(), e);
            throw e;
        }
    }

    // 설비 점검 알림 레이아웃 블록 생성
    private List<LayoutBlock> generateLayoutBlock(String equipmentName, String zoneName, LocalDate expectedMaintenanceDate, long daysUntilMaintenance) {
        return Blocks.asBlocks(
                getHeader("⚠️ 설비 점검 알림"),
                Blocks.divider(),
                getSection(generateMaintenanceMessage(equipmentName, zoneName, expectedMaintenanceDate, daysUntilMaintenance))
        );
    }

    // 설비 점검 알림 메시지 생성
    private String generateMaintenanceMessage(String equipmentName, String zoneName, LocalDate expectedMaintenanceDate, long daysUntilMaintenance) {
        StringBuilder sb = new StringBuilder();
        sb.append("*[설비명]*").append(NEW_LINE)
                .append(equipmentName).append(NEW_LINE).append(NEW_LINE)
                .append("*[공간]*").append(NEW_LINE)
                .append(zoneName).append(NEW_LINE).append(NEW_LINE)
                .append("*[예상 점검일]*").append(NEW_LINE)
                .append(expectedMaintenanceDate).append(NEW_LINE).append(NEW_LINE)
                .append("*[남은 기간]*").append(NEW_LINE)
                .append("🚨D-").append(daysUntilMaintenance).append(NEW_LINE);

        return sb.toString();
    }

    // 설비 점검 알림 레이아웃 블록 헤더 생성
    private LayoutBlock getHeader(String text) {
        return Blocks.header(h -> h.text(
                BlockCompositions.plainText(pt -> pt.emoji(true)
                        .text(text))));
    }

    // 설비 점검 알림 레이아웃 블록 섹션 생성
    private LayoutBlock getSection(String message) {
        return Blocks.section(s ->
                s.text(BlockCompositions.markdownText(message)));
    }

    // 설비 점검 알림 전송 조건 확인
    public boolean shouldSendAlert(LocalDate expectedMaintenanceDate) {
        if (expectedMaintenanceDate == null) {
            throw new NullPointerException("Expected maintenance date cannot be null");
        }
        long daysUntilMaintenance = ChronoUnit.DAYS.between(LocalDate.now(), expectedMaintenanceDate);
        return daysUntilMaintenance == 5 || daysUntilMaintenance == 3;
    }

    // 설비 점검 알림 남은 기간 계산 (예상 점검일자 - 오늘 날짜)
    public long getDaysUntilMaintenance(LocalDate expectedMaintenanceDate) {
        if (expectedMaintenanceDate == null) {
            throw new NullPointerException("Expected maintenance date cannot be null");
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), expectedMaintenanceDate);
    }
} 