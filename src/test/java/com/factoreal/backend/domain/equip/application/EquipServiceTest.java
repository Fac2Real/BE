package com.factoreal.backend.domain.equip.application;

import com.factoreal.backend.domain.equip.dto.request.EquipCreateRequest;
import com.factoreal.backend.domain.equip.dto.request.EquipUpdateRequest;
import com.factoreal.backend.domain.equip.dto.request.EquipCheckDateRequest;
import com.factoreal.backend.domain.equip.dto.response.EquipInfoResponse;
import com.factoreal.backend.domain.equip.dto.response.EquipWithSensorsResponse;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.equip.entity.EquipHistory;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.sensor.application.SensorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipServiceTest {

    @InjectMocks
    private EquipService equipService;

    @Mock
    private ZoneRepoService zoneRepoService;

    @Mock
    private EquipRepoService equipRepoService;

    @Mock
    private EquipHistoryRepoService equipHistoryRepoService;

    @Mock
    private SensorService sensorService;

    private Zone zone;
    private Equip equip;
    private String zoneId;
    private String zoneName;
    private String equipId;
    private String equipName;

    @BeforeEach
    void setUp() {
        zoneId = "zone001";
        zoneName = "조립라인1";
        equipId = "equip001";
        equipName = "설비1";

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
    @DisplayName("설비 생성 테스트")
    class CreateEquipTest {

        @Test
        @DisplayName("정상적인 설비 생성")
        void createEquip_Success() {
            // given
            EquipCreateRequest request = new EquipCreateRequest();
            request.setEquipName(equipName);
            request.setZoneName(zoneName);

            given(zoneRepoService.findByZoneName(zoneName)).willReturn(zone);
            given(equipRepoService.save(any(Equip.class))).willReturn(equip);

            // when
            EquipInfoResponse response = equipService.createEquip(request);

            // then
            assertThat(response.getEquipName()).isEqualTo(equipName);
            assertThat(response.getZoneName()).isEqualTo(zoneName);
            verify(equipRepoService, times(1)).save(any(Equip.class));
        }

        @Test
        @DisplayName("존재하지 않는 구역으로 설비 생성 시도")
        void createEquip_WithNonExistentZone() {
            // given
            EquipCreateRequest request = new EquipCreateRequest();
            request.setEquipName(equipName);
            request.setZoneName("존재하지 않는 구역");

            given(zoneRepoService.findByZoneName(anyString()))
                    .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

            // when & then
            assertThrows(ResponseStatusException.class, () -> equipService.createEquip(request));
            verify(equipRepoService, never()).save(any(Equip.class)); // 저장 안됨
        }
    }

    @Nested
    @DisplayName("설비 업데이트 테스트")
    class UpdateEquipTest {

        @Test
        @DisplayName("정상적인 설비명 업데이트")
        void updateEquip_Success() {
            // given
            String newEquipName = "새로운설비1";
            EquipUpdateRequest request = new EquipUpdateRequest();
            request.setEquipName(newEquipName);

            Equip updatedEquip = Equip.builder()
                    .equipId(equipId)
                    .equipName(newEquipName)
                    .zone(zone)
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipRepoService.save(any(Equip.class))).willReturn(updatedEquip);
            given(zoneRepoService.findById(zoneId)).willReturn(zone);

            // when
            EquipInfoResponse response = equipService.updateEquip(equipId, request);

            // then
            assertThat(response.getEquipName()).isEqualTo(newEquipName);
            verify(equipRepoService, times(1)).save(any(Equip.class));
        }

        @Test
        @DisplayName("존재하지 않는 설비 업데이트 시도")
        void updateEquip_WithNonExistentEquip() {
            // given
            EquipUpdateRequest request = new EquipUpdateRequest();
            request.setEquipName("새로운설비1");

            given(equipRepoService.findById(anyString()))
                    .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

            // when & then
            assertThrows(ResponseStatusException.class, () -> equipService.updateEquip("wrong_id", request));
            verify(equipRepoService, never()).save(any(Equip.class));
        }
    }

    @Nested
    @DisplayName("설비 실제 점검일자 업데이트 테스트")
    class UpdateCheckDateTest {

        @Test
        @DisplayName("정상적인 실제 점검일자 업데이트")
        void updateCheckDate_Success() {
            // given
            LocalDate checkDate = LocalDate.now();
            EquipCheckDateRequest request = new EquipCheckDateRequest();
            request.setCheckDate(checkDate);

            EquipHistory history = EquipHistory.builder()
                    .equip(equip)
                    .checkDate(null)
                    .build();

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.of(history));
            given(zoneRepoService.findById(zoneId)).willReturn(zone);

            // when
            EquipInfoResponse response = equipService.updateCheckDateEquip(equipId, request);

            // then
            verify(equipHistoryRepoService, times(1)).save(any(EquipHistory.class));
            assertThat(response.getEquipId()).isEqualTo(equipId);
        }

        @Test
        @DisplayName("미점검 이력이 없는 경우")
        void updateCheckDate_WithNoUncheckedHistory() {
            // given
            LocalDate checkDate = LocalDate.now();
            EquipCheckDateRequest request = new EquipCheckDateRequest();
            request.setCheckDate(checkDate);

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(equipHistoryRepoService.findLatestUncheckedByEquipId(equipId))
                    .willReturn(Optional.empty());
            given(zoneRepoService.findById(zoneId)).willReturn(zone);

            // when
            EquipInfoResponse response = equipService.updateCheckDateEquip(equipId, request);

            // then
            verify(equipHistoryRepoService, never()).save(any(EquipHistory.class));
            assertThat(response.getEquipId()).isEqualTo(equipId);
        }

        @Test
        @DisplayName("점검일자가 null인 경우")
        void updateCheckDate_WithNullDate() {
            // given
            EquipCheckDateRequest request = new EquipCheckDateRequest();
            request.setCheckDate(null);

            given(equipRepoService.findById(equipId)).willReturn(equip);
            given(zoneRepoService.findById(zoneId)).willReturn(zone);

            // when
            EquipInfoResponse response = equipService.updateCheckDateEquip(equipId, request);

            // then
            verify(equipHistoryRepoService, never()).save(any(EquipHistory.class));
            assertThat(response.getEquipId()).isEqualTo(equipId);
        }
    }

    @Nested
    @DisplayName("설비 조회 테스트")
    class GetEquipsTest {

        @Test
        @DisplayName("모든 설비 조회")
        void getAllEquips_Success() {
            // given
            List<EquipInfoResponse> expectedResponses = Arrays.asList(
                    EquipInfoResponse.fromEntity(equip, zone),
                    EquipInfoResponse.fromEntity(
                            Equip.builder()
                                    .equipId("equip002")
                                    .equipName("설비2")
                                    .zone(zone)
                                    .build(),
                            zone
                    )
            );

            given(equipRepoService.findAll()).willReturn(expectedResponses);

            // when
            List<EquipInfoResponse> responses = equipService.getAllEquips();

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getEquipId()).isEqualTo(equipId);
        }

        @Test
        @DisplayName("특정 구역의 설비 조회")
        void getEquipsByZoneId_Success() {
            // given
            LocalDate lastCheckDate = LocalDate.now().minusDays(5);
            List<Equip> zoneEquips = Arrays.asList(equip); // equip 하나만 있는 리스트

            given(zoneRepoService.findById(zoneId)).willReturn(zone);
            given(equipRepoService.findEquipsByZone(zone)).willReturn(zoneEquips);
            given(equipHistoryRepoService.findLatestCheckedByEquipId(equipId))
                    .willReturn(Optional.of(EquipHistory.builder()
                            .checkDate(lastCheckDate)
                            .build()));

            // when
            List<EquipWithSensorsResponse> responses = equipService.getEquipsByZoneId(zoneId);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getEquipId()).isEqualTo(equipId);
            assertThat(responses.get(0).getLastCheckDate()).isEqualTo(lastCheckDate);
        }

        @Test
        @DisplayName("점검 이력이 없는 설비 조회")
        void getEquipsByZoneId_WithNoCheckHistory() {
            // given
            List<Equip> zoneEquips = Arrays.asList(equip);

            given(zoneRepoService.findById(zoneId)).willReturn(zone);
            given(equipRepoService.findEquipsByZone(zone)).willReturn(zoneEquips);
            given(equipHistoryRepoService.findLatestCheckedByEquipId(equipId))
                    .willReturn(Optional.empty());

            // when
            List<EquipWithSensorsResponse> responses = equipService.getEquipsByZoneId(zoneId);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getEquipId()).isEqualTo(equipId);
            assertThat(responses.get(0).getLastCheckDate()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 구역의 설비 조회")
        void getEquipsByZoneId_WithNonExistentZone() {
            // given
            given(zoneRepoService.findById(anyString()))
                    .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

            // when & then
            assertThrows(ResponseStatusException.class,
                    () -> equipService.getEquipsByZoneId("wrong_zone_id"));
        }
    }
}