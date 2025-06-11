package com.factoreal.backend.messaging.slack.api;

import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlackEquipAlarmServiceTest {

    @InjectMocks
    private SlackEquipAlarmService slackEquipAlarmService;

    @Mock
    private Slack slackClient;

    private final String WEBHOOK_URL = "https://hooks.slack.com/services/test/webhook";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(slackEquipAlarmService, "SLACK_WEBHOOK_EQUIP_URL", WEBHOOK_URL);
    }

    @Nested
    @DisplayName("sendEquipmentMaintenanceAlert 메소드 테스트")
    class SendEquipmentMaintenanceAlertTest {

        @Test
        @DisplayName("정상적인 알림 전송 테스트")
        void whenValidInput_thenSendAlert() throws IOException {
            // given
            String equipmentName = "설비1";
            String zoneName = "구역1";
            LocalDate expectedDate = LocalDate.now().plusDays(5);
            long daysUntilMaintenance = 5L;

            WebhookResponse mockResponse = mock(WebhookResponse.class);
            when(slackClient.send(eq(WEBHOOK_URL), any(Payload.class))).thenReturn(mockResponse);

            // when
            slackEquipAlarmService.sendEquipmentMaintenanceAlert(
                    equipmentName, zoneName, expectedDate, daysUntilMaintenance);

            // then
            verify(slackClient).send(eq(WEBHOOK_URL), any(Payload.class));
        }

        @Test
        @DisplayName("null 입력값 처리 테스트")
        void whenNullInput_thenThrowException() {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(5);
            
            // when & then
            assertThatThrownBy(() -> 
                slackEquipAlarmService.sendEquipmentMaintenanceAlert(
                    null, "구역1", expectedDate, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Equipment name cannot be null");

            assertThatThrownBy(() -> 
                slackEquipAlarmService.sendEquipmentMaintenanceAlert(
                    "설비1", null, expectedDate, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Zone name cannot be null");

            assertThatThrownBy(() -> 
                slackEquipAlarmService.sendEquipmentMaintenanceAlert(
                    "설비1", "구역1", null, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected date cannot be null");
        }

        @Test
        @DisplayName("Slack API 호출 실패 시 예외 처리 테스트")
        void whenSlackApiFails_thenThrowException() throws IOException {
            // given
            String equipmentName = "설비1";
            String zoneName = "구역1";
            LocalDate expectedDate = LocalDate.now().plusDays(5);
            long daysUntilMaintenance = 5L;
            
            doThrow(new IOException("Slack API 호출 실패"))
                .when(slackClient)
                .send(eq(WEBHOOK_URL), any(Payload.class));

            // when & then
            assertThatThrownBy(() -> 
                slackEquipAlarmService.sendEquipmentMaintenanceAlert(
                    equipmentName, zoneName, expectedDate, daysUntilMaintenance))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Slack API 호출 실패");
        }
    }

    @Nested
    @DisplayName("shouldSendAlert 메소드 테스트")
    class ShouldSendAlertTest {

        @Test
        @DisplayName("D-5일 때 알림 발송 여부 테스트")
        void whenD5_thenShouldSendAlert() {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(5);

            // when
            boolean shouldSend = slackEquipAlarmService.shouldSendAlert(expectedDate);

            // then
            assertThat(shouldSend).isTrue();
        }

        @Test
        @DisplayName("D-3일 때 알림 발송 여부 테스트")
        void whenD3_thenShouldSendAlert() {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(3);

            // when
            boolean shouldSend = slackEquipAlarmService.shouldSendAlert(expectedDate);

            // then
            assertThat(shouldSend).isTrue();
        }

        @Test
        @DisplayName("D-5, D-3이 아닐 때 알림 발송하지 않음 테스트")
        void whenNotD5orD3_thenShouldNotSendAlert() {
            // given
            LocalDate expectedDate1 = LocalDate.now().plusDays(4);
            LocalDate expectedDate2 = LocalDate.now().plusDays(6);
            LocalDate expectedDate3 = LocalDate.now().plusDays(2);

            // when & then
            assertThat(slackEquipAlarmService.shouldSendAlert(expectedDate1)).isFalse();
            assertThat(slackEquipAlarmService.shouldSendAlert(expectedDate2)).isFalse();
            assertThat(slackEquipAlarmService.shouldSendAlert(expectedDate3)).isFalse();
        }

        @Test
        @DisplayName("null 입력값 처리 테스트")
        void whenNullInput_thenThrowException() {
            // when & then
            assertThatThrownBy(() -> slackEquipAlarmService.shouldSendAlert(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Expected maintenance date cannot be null");
        }
    }

    @Nested
    @DisplayName("getDaysUntilMaintenance 메소드 테스트")
    class GetDaysUntilMaintenanceTest {

        @Test
        @DisplayName("정상적인 날짜 차이 계산 테스트")
        void whenValidDate_thenCalculateDays() {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(5);

            // when
            long days = slackEquipAlarmService.getDaysUntilMaintenance(expectedDate);

            // then
            assertThat(days).isEqualTo(5L);
        }

        @Test
        @DisplayName("과거 날짜에 대한 계산 테스트")
        void whenPastDate_thenReturnNegativeDays() {
            // given
            LocalDate pastDate = LocalDate.now().minusDays(5);

            // when
            long days = slackEquipAlarmService.getDaysUntilMaintenance(pastDate);

            // then
            assertThat(days).isEqualTo(-5L);
        }

        @Test
        @DisplayName("당일에 대한 계산 테스트")
        void whenToday_thenReturnZero() {
            // given
            LocalDate today = LocalDate.now();

            // when
            long days = slackEquipAlarmService.getDaysUntilMaintenance(today);

            // then
            assertThat(days).isEqualTo(0L);
        }

        @Test
        @DisplayName("null 입력값 처리 테스트")
        void whenNullInput_thenThrowException() {
            // when & then
            assertThatThrownBy(() -> 
                slackEquipAlarmService.getDaysUntilMaintenance(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Expected maintenance date cannot be null");
        }

        @Test
        @DisplayName("최대값 경계 테스트")
        void whenMaxDate_thenCalculateDays() {
            // given
            LocalDate maxDate = LocalDate.now().plusYears(100);

            // when
            long days = slackEquipAlarmService.getDaysUntilMaintenance(maxDate);

            // then
            assertThat(days).isEqualTo(ChronoUnit.DAYS.between(LocalDate.now(), maxDate));
        }
    }
} 