package com.factoreal.backend.domain.equip.application;

import com.factoreal.backend.domain.equip.dto.response.EquipInfoResponse;
import com.factoreal.backend.domain.equip.dto.response.MaintenancePredictionResponse;
import com.factoreal.backend.domain.equip.dto.response.LatestMaintenancePredictionResponse;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.equip.entity.EquipHistory;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.messaging.slack.api.SlackEquipAlarmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class EquipMaintenanceServiceTest {

    @InjectMocks
    private EquipMaintenanceService equipMaintenanceService;

    @Mock
    private EquipRepoService equipRepoService;

    @Mock
    private EquipHistoryRepoService equipHistoryRepoService;

    @Mock
    private SlackEquipAlarmService slackEquipAlarmService;

    @Mock
    private RestTemplate restTemplate;

    private final String fastApiBaseUrl = "http://localhost:8000";
    private final String predictEndpoint = "/predict-from-s3";

    private String equipId;
    private String equipName;
    private String zoneId;
    private String zoneName;
    private Equip equip;
    private Zone zone;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(equipMaintenanceService, "fastApiBaseUrl", fastApiBaseUrl);
        ReflectionTestUtils.setField(equipMaintenanceService, "predictEndpoint", predictEndpoint);

        // 테스트에서 공통으로 사용할 데이터 설정
        equipId = "equip_001";
        equipName = "설비1";
        zoneId = "zone_001";
        zoneName = "조립라인1";

        zone = Zone.builder()
                .zoneId(zoneId)
                .zoneName(zoneName)
                .build();

        equip = Equip.builder()
                .equipId(equipId)
                .equipName(equipName)
                .zone(zone)
                .build();
    }

    @Nested
    @DisplayName("processMaintenancePrediction 메서드 테스트")
    class ProcessMaintenancePredictionTest {

    @Test
        @DisplayName("Case 1: 첫 예측값일 때 - 이력 저장 및 알림 발송 (D-3)")
        void firstPrediction_shouldSaveAndSendAlert_D3() throws IOException {
        // given
            LocalDate expectedDate = LocalDate.now().plusDays(3);  // D-3
            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.empty());
            given(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate)).willReturn(3L);

            // when
            equipMaintenanceService.processMaintenancePrediction(equipId, expectedDate);

            // then
            verify(equipHistoryRepoService, times(1)).save(any(EquipHistory.class));
            verify(slackEquipAlarmService, times(1))
                    .sendEquipmentMaintenanceAlert(
                            eq(equipName),
                            eq(zoneName),
                            eq(expectedDate),
                            eq(3L)
                    );
        }

        @Test
        @DisplayName("Case 1: 첫 예측값일 때 - 이력 저장 및 알림 발송 (D-7)")
        void firstPrediction_shouldSaveAndSendAlert_D7() throws IOException {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(7);  // D-7
            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.empty());
            given(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate)).willReturn(7L);

            // when
            equipMaintenanceService.processMaintenancePrediction(equipId, expectedDate);

            // then
            verify(equipHistoryRepoService, times(1)).save(any(EquipHistory.class));
            verify(slackEquipAlarmService, times(1))
                    .sendEquipmentMaintenanceAlert(
                            eq(equipName),
                            eq(zoneName),
                            eq(expectedDate),
                            eq(7L)
                    );
        }

        @Test
        @DisplayName("Case 1: 첫 예측값일 때 슬랙 알림 발송 실패하는 경우 - 이력은 저장")
        void firstPrediction_whenSlackAlertFails_shouldStillSaveHistory() throws IOException {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(3);
            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.empty());
            given(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate)).willReturn(3L);
            doThrow(new IOException("Slack API 호출 실패"))
                    .when(slackEquipAlarmService)
                    .sendEquipmentMaintenanceAlert(
                            eq(equipName),
                            eq(zoneName),
                            eq(expectedDate),
                            eq(3L)
                    );

        // when
            equipMaintenanceService.processMaintenancePrediction(equipId, expectedDate);

        // then
            verify(equipHistoryRepoService, times(1)).save(any(EquipHistory.class));
        verify(slackEquipAlarmService, times(1))
                    .sendEquipmentMaintenanceAlert(
                            eq(equipName),
                            eq(zoneName),
                            eq(expectedDate),
                            eq(3L)
                    );
    }

    @Test
        @DisplayName("Case 2: 기존 이력이 있고 D-5보다 짧은 경우 - 기존 이력 유지")
        void existingPrediction_lessThanD5_shouldKeepExisting() throws IOException {
        // given
            LocalDate currentDate = LocalDate.now().plusDays(6);
            LocalDate newDate = LocalDate.now().plusDays(4);

            EquipHistory currentHistory = EquipHistory.builder()
                    .equip(equip)
                    .accidentDate(currentDate)
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.of(currentHistory));
            given(slackEquipAlarmService.getDaysUntilMaintenance(newDate)).willReturn(4L);

            // when
            equipMaintenanceService.processMaintenancePrediction(equipId, newDate);

            // then
            verify(equipHistoryRepoService, never()).save(any(EquipHistory.class));
            assertThat(currentHistory.getAccidentDate()).isEqualTo(currentDate);
        }

        @Test
        @DisplayName("Case 2: 기존 이력이 있고 D-5보다 길며 현재 시점과 더 가까운 경우 - 이력 업데이트")
        void existingPrediction_moreThanD5AndCloser_shouldUpdate() throws IOException {
            // given
            LocalDate currentDate = LocalDate.now().plusDays(10);
            LocalDate newDate = LocalDate.now().plusDays(7);

            EquipHistory currentHistory = EquipHistory.builder()
                    .equip(equip)
                    .accidentDate(currentDate)
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.of(currentHistory));
            given(slackEquipAlarmService.getDaysUntilMaintenance(newDate)).willReturn(7L);
            given(slackEquipAlarmService.getDaysUntilMaintenance(currentDate)).willReturn(10L);

        // when
            equipMaintenanceService.processMaintenancePrediction(equipId, newDate);

        // then
            verify(equipHistoryRepoService, times(1)).save(currentHistory);
            assertThat(currentHistory.getAccidentDate()).isEqualTo(newDate);
    }

    @Test
        @DisplayName("Case 2: 기존 이력이 있고 D-5보다 길며 현재 시점과 더 먼 경우 - 기존 이력 유지")
        void existingPrediction_moreThanD5AndFurther_shouldKeepExisting() throws IOException {
            // given
            LocalDate currentDate = LocalDate.now().plusDays(7);
            LocalDate newDate = LocalDate.now().plusDays(10);

            EquipHistory currentHistory = EquipHistory.builder()
                    .equip(equip)
                    .accidentDate(currentDate)
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.of(currentHistory));
            given(slackEquipAlarmService.getDaysUntilMaintenance(newDate)).willReturn(10L);
            given(slackEquipAlarmService.getDaysUntilMaintenance(currentDate)).willReturn(7L);

            // when
            equipMaintenanceService.processMaintenancePrediction(equipId, newDate);

            // then
            verify(equipHistoryRepoService, never()).save(any(EquipHistory.class));
            assertThat(currentHistory.getAccidentDate()).isEqualTo(currentDate);
        }
    }

    @Nested
    @DisplayName("정기 알림 발송 테스트")
    class RegularAlertTest {

        @Test
        @DisplayName("D-5일 때 알림 발송")
        void whenD5_shouldSendAlert() throws IOException {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(5);
            lenient().when(equipRepoService.findById(equipId)).thenReturn(equip);
            lenient().when(slackEquipAlarmService.shouldSendAlert(expectedDate)).thenReturn(true);
            lenient().when(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate)).thenReturn(5L);
            lenient().when(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .thenReturn(Optional.empty());

            // when
            equipMaintenanceService.processMaintenancePrediction(equipId, expectedDate);

            // then
            verify(slackEquipAlarmService).sendEquipmentMaintenanceAlert(
                    equip.getEquipName(),
                    equip.getZone().getZoneName(),
                    expectedDate,
                    5L
            );
        }

        @Test
        @DisplayName("D-4일 때 알림 미발송")
        void whenD4_shouldNotSendAlert() throws IOException {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(4);
            EquipHistory existingHistory = EquipHistory.builder()
                    .equip(equip)
                    .accidentDate(expectedDate)
                    .build();

            lenient().when(equipRepoService.findById(equipId)).thenReturn(equip);
            lenient().when(slackEquipAlarmService.shouldSendAlert(expectedDate)).thenReturn(false);
            lenient().when(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate)).thenReturn(4L);
            lenient().when(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .thenReturn(Optional.of(existingHistory));

            // when
            equipMaintenanceService.processMaintenancePrediction(equipId, expectedDate);

            // then
            verify(slackEquipAlarmService, never()).sendEquipmentMaintenanceAlert(
                    eq(equip.getEquipName()),
                    eq(equip.getZone().getZoneName()),
                    eq(expectedDate),
                    eq(4L)
            );
            verify(equipHistoryRepoService, never()).save(any(EquipHistory.class));
        }

        @Test
        @DisplayName("D-3일 때 알림 발송")
        void whenD3_shouldSendAlert() throws IOException {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(3);
            lenient().when(equipRepoService.findById(equipId)).thenReturn(equip);
            lenient().when(slackEquipAlarmService.shouldSendAlert(expectedDate)).thenReturn(true);
            lenient().when(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate)).thenReturn(3L);
            lenient().when(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .thenReturn(Optional.empty());

            // when
            equipMaintenanceService.processMaintenancePrediction(equipId, expectedDate);

            // then
            verify(slackEquipAlarmService).sendEquipmentMaintenanceAlert(
                    equip.getEquipName(),
                    equip.getZone().getZoneName(),
                    expectedDate,
                    3L
            );
        }

        @Test
        @DisplayName("정기 알림 발송 실패 시 예외 처리")
        void whenRegularAlertFails_shouldHandleException() throws IOException {
        // given
            LocalDate expectedDate = LocalDate.now().plusDays(5);
            lenient().when(equipRepoService.findById(equipId)).thenReturn(equip);
            lenient().when(slackEquipAlarmService.shouldSendAlert(expectedDate)).thenReturn(true);
            lenient().when(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate)).thenReturn(5L);
            lenient().when(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .thenReturn(Optional.empty());
            lenient().doThrow(new IOException("Slack API 호출 실패"))
                    .when(slackEquipAlarmService)
                    .sendEquipmentMaintenanceAlert(
                            eq(equip.getEquipName()),
                            eq(equip.getZone().getZoneName()),
                            eq(expectedDate),
                            eq(5L)
                    );

            // when
            equipMaintenanceService.processMaintenancePrediction(equipId, expectedDate);

            // then
            verify(equipHistoryRepoService).save(any(EquipHistory.class));
            verify(slackEquipAlarmService).sendEquipmentMaintenanceAlert(
                    equip.getEquipName(),
                    equip.getZone().getZoneName(),
                    expectedDate,
                    5L
            );
        }
    }

    @Nested
    @DisplayName("calculateExpectedMaintenanceDate 메서드 테스트")
    class CalculateExpectedMaintenanceDateTest {

        @Test
        @DisplayName("남은 일수로 예상 점검일자 계산")
        void calculateExpectedMaintenanceDate_ShouldReturnCorrectDate() {
            // given
            int remainingDays = 5;
            LocalDate expectedDate = LocalDate.now().plusDays(remainingDays);

            // when
            LocalDate result = ReflectionTestUtils.invokeMethod(
                    equipMaintenanceService,
                    "calculateExpectedMaintenanceDate",
                    remainingDays
            );

            // then
            assertThat(result).isEqualTo(expectedDate);
        }
    }

    @Nested
    @DisplayName("getLatestMaintenancePrediction 메서드 테스트")
    class GetLatestMaintenancePredictionTest {

        @Test
        @DisplayName("미점검 이력이 있는 경우, 해당 이력의 예상 점검일자를 반환한다")
        void whenUncheckedHistoryExists_returnsThatHistory() {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(5);
            EquipHistory uncheckedHistory = EquipHistory.builder()
                    .equip(equip)
                    .accidentDate(expectedDate)
                    .checkDate(null)
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.of(uncheckedHistory));
            given(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate))
                    .willReturn(5L);

            // when
            LatestMaintenancePredictionResponse response =
                    equipMaintenanceService.getLatestMaintenancePrediction(equipId, zoneId);

            // then
            assertThat(response.getEquipId()).isEqualTo(equipId);
            assertThat(response.getEquipName()).isEqualTo(equipName);
            assertThat(response.getZoneId()).isEqualTo(zoneId);
            assertThat(response.getZoneName()).isEqualTo(zoneName);
            assertThat(response.getExpectedMaintenanceDate()).isEqualTo(expectedDate);
            assertThat(response.getDaysUntilMaintenance()).isEqualTo(5L);
        }

        @Test
        @DisplayName("모든 이력이 점검 완료된 경우, 가장 최근 이력의 예상 점검일자를 반환한다")
        void whenAllHistoriesChecked_returnsLatestHistory() {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(10);
            EquipHistory checkedHistory = EquipHistory.builder()
                    .equip(equip)
                    .accidentDate(expectedDate)
                    .checkDate(LocalDate.now())
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.empty());
            given(equipHistoryRepoService.findLatestByEquipId(equipId))
                    .willReturn(Optional.of(checkedHistory));
            given(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate))
                    .willReturn(10L);

            // when
            LatestMaintenancePredictionResponse response =
                    equipMaintenanceService.getLatestMaintenancePrediction(equipId, zoneId);

            // then
            assertThat(response.getEquipId()).isEqualTo(equipId);
            assertThat(response.getExpectedMaintenanceDate()).isEqualTo(expectedDate);
            assertThat(response.getDaysUntilMaintenance()).isEqualTo(10L);
        }

        @Test
        @DisplayName("이력이 전혀 없는 경우, NotFound 예외를 던진다")
        void whenNoHistoryExists_throwsNotFoundException() {
            // given
            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.empty());
            given(equipHistoryRepoService.findLatestByEquipId(equipId))
                    .willReturn(Optional.empty());

            // when & then
            assertThrows(ResponseStatusException.class, () ->
                            equipMaintenanceService.getLatestMaintenancePrediction(equipId, zoneId),
                    "최근 예상 점검일자를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("잘못된 zoneId가 입력된 경우, BadRequest 예외를 던진다")
        void whenInvalidZoneId_throwsBadRequestException() {
            // given
            String wrongZoneId = "wrong_zone_id";
            Zone wrongZone = Zone.builder()
                    .zoneId(wrongZoneId)
                    .zoneName("Wrong Zone")
                    .build();
            Equip equipWithWrongZone = Equip.builder()
                    .equipId(equipId)
                    .equipName(equipName)
                    .zone(wrongZone)
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equipWithWrongZone);

            // when & then
            assertThrows(ResponseStatusException.class, () ->
                            equipMaintenanceService.getLatestMaintenancePrediction(equipId, zoneId),
                    "설비가 해당 공간에 속하지 않습니다.");
        }

        // 경계값과 예외값에 대한 테스트 케이스
        @Test
        @DisplayName("설비 이름이 null인 필드를 포함할 때의 처리를 검증한다")
        void whenEquipHasNullFields_handlesGracefully() {
            // given
            LocalDate expectedDate = LocalDate.now().plusDays(5);
            Equip equipWithNulls = Equip.builder()
                    .equipId(equipId)
                    .equipName(null)  // null 이름
                    .zone(Zone.builder()
                            .zoneId(zoneId)
                            .zoneName(null)  // null 구역 이름
                            .build())
                    .build();

            EquipHistory uncheckedHistory = EquipHistory.builder()
                    .equip(equipWithNulls)
                    .accidentDate(expectedDate)
                    .checkDate(null)
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equipWithNulls);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.of(uncheckedHistory));
            given(slackEquipAlarmService.getDaysUntilMaintenance(expectedDate))
                    .willReturn(5L);

            // when
            LatestMaintenancePredictionResponse response =
                    equipMaintenanceService.getLatestMaintenancePrediction(equipId, zoneId);

            // then
            assertThat(response.getEquipId()).isEqualTo(equipId);
            assertThat(response.getEquipName()).isNull();
            assertThat(response.getZoneId()).isEqualTo(zoneId);
            assertThat(response.getZoneName()).isNull();
            assertThat(response.getExpectedMaintenanceDate()).isEqualTo(expectedDate);
            assertThat(response.getDaysUntilMaintenance()).isEqualTo(5L);
        }

        @Test
        @DisplayName("과거 날짜의 점검 예정일이 입력된 경우를 검증한다")
        void whenPastMaintenanceDate_handlesCorrectly() {
            // given
            LocalDate pastDate = LocalDate.now().minusDays(5);
            EquipHistory historyWithPastDate = EquipHistory.builder()
                    .equip(equip)
                    .accidentDate(pastDate)
                    .checkDate(null)
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.of(historyWithPastDate));
            given(slackEquipAlarmService.getDaysUntilMaintenance(pastDate))
                    .willReturn(-5L);  // 과거 날짜는 음수 일수 반환

            // when
            LatestMaintenancePredictionResponse response =
                    equipMaintenanceService.getLatestMaintenancePrediction(equipId, zoneId);

            // then
            assertThat(response.getEquipId()).isEqualTo(equipId);
            assertThat(response.getEquipName()).isEqualTo(equipName);
            assertThat(response.getZoneId()).isEqualTo(zoneId);
            assertThat(response.getZoneName()).isEqualTo(zoneName);
            assertThat(response.getExpectedMaintenanceDate()).isEqualTo(pastDate);
            assertThat(response.getDaysUntilMaintenance()).isEqualTo(-5L);
        }

        @Test
        @DisplayName("매우 먼 미래의 점검 예정일이 입력된 경우를 검증한다")
        void whenFarFutureMaintenanceDate_handlesCorrectly() {
            // given
            LocalDate farFutureDate = LocalDate.now().plusYears(3);  // 3년 후
            EquipHistory historyWithFarFutureDate = EquipHistory.builder()
                    .equip(equip)
                    .accidentDate(farFutureDate)
                    .checkDate(null)
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.of(historyWithFarFutureDate));
            given(slackEquipAlarmService.getDaysUntilMaintenance(farFutureDate))
                    .willReturn(365 * 3L);  // 약 365일

            // when
            LatestMaintenancePredictionResponse response =
                    equipMaintenanceService.getLatestMaintenancePrediction(equipId, zoneId);

            // then
            assertThat(response.getEquipId()).isEqualTo(equipId);
            assertThat(response.getEquipName()).isEqualTo(equipName);
            assertThat(response.getZoneId()).isEqualTo(zoneId);
            assertThat(response.getZoneName()).isEqualTo(zoneName);
            assertThat(response.getExpectedMaintenanceDate()).isEqualTo(farFutureDate);
            assertThat(response.getDaysUntilMaintenance()).isEqualTo(365 * 3L);
        }

        @Test
        @DisplayName("checkDate가 accidentDate보다 이후인 경우를 검증한다")
        void whenCheckDateAfterAccidentDate_handlesCorrectly() {
            // given
            LocalDate accidentDate = LocalDate.now().plusDays(5);
            LocalDate checkDate = LocalDate.now().plusDays(10);  // 예상 점검일보다 늦게 실제 점검
            EquipHistory historyWithLateCheck = EquipHistory.builder()
                    .equip(equip)
                    .accidentDate(accidentDate)
                    .checkDate(checkDate)
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.empty());
            given(equipHistoryRepoService.findLatestByEquipId(equipId))
                    .willReturn(Optional.of(historyWithLateCheck));
            given(slackEquipAlarmService.getDaysUntilMaintenance(accidentDate))
                    .willReturn(5L);

        // when
            LatestMaintenancePredictionResponse response =
                    equipMaintenanceService.getLatestMaintenancePrediction(equipId, zoneId);

        // then
            assertThat(response.getEquipId()).isEqualTo(equipId);
            assertThat(response.getEquipName()).isEqualTo(equipName);
            assertThat(response.getZoneId()).isEqualTo(zoneId);
            assertThat(response.getZoneName()).isEqualTo(zoneName);
            assertThat(response.getExpectedMaintenanceDate()).isEqualTo(accidentDate);
            assertThat(response.getDaysUntilMaintenance()).isEqualTo(5L);
        }
    }
} 