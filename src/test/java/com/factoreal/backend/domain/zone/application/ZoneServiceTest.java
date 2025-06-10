package com.factoreal.backend.domain.zone.application;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.request.AbnormalPagingRequest;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.equip.application.EquipRepoService;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.zone.dto.request.ZoneCreateRequest;
import com.factoreal.backend.domain.zone.dto.request.ZoneUpdateRequest;
import com.factoreal.backend.domain.zone.dto.response.ZoneDetailResponse;
import com.factoreal.backend.domain.zone.dto.response.ZoneInfoResponse;
import com.factoreal.backend.domain.zone.dto.response.ZoneLogResponse;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.exception.dto.BadRequestException;
import com.factoreal.backend.global.exception.dto.DuplicateResourceException;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @InjectMocks
    private ZoneService zoneService;

    @Mock
    private SensorRepoService sensorRepoService;
    @Mock
    private ZoneRepoService zoneRepoService;
    @Mock
    private EquipRepoService equipRepoService;
    @Mock
    private AbnormalLogRepoService abnormalLogRepoService;


    // 1. createZone

    /* ---------- 헬퍼 ---------- */
    private Zone zone(String id, String name) {
        return Zone.builder().zoneId(id).zoneName(name).build();
    }

    /* ===== 공용 더미 ===== update, create 등등에 쓰임 */
    private Zone zone() { return new Zone("Z1", "대기실"); }


    /* 1. createZone_success ------------------------------------ */
    @Test
    void createZone_success() {
        ZoneCreateRequest req = new ZoneCreateRequest("대기실");

        // validate 는 예외없음
        doNothing().when(zoneRepoService).validateZoneName("대기실");

        // save 시 id 를 채워서 반환
        when(zoneRepoService.save(any(Zone.class)))
                .thenAnswer(inv -> {
                    Zone z = inv.getArgument(0, Zone.class);
                    z.setZoneId("zone-1");
                    return z;
                });

        ZoneInfoResponse res = zoneService.createZone(req);

        // 생성한 존 정보 가져와보고, 체킹
        assertThat(res.getZoneId()).isEqualTo("zone-1");
        assertThat(res.getZoneName()).isEqualTo("대기실");
    }

    @Test
    void createZone_blankName_throwsException() {
        ZoneCreateRequest req = new ZoneCreateRequest("");
        // 예외 로직이 서비스에 추가되어야 함
        doThrow(new BadRequestException("공간이름이 공백일 수 없습니다."))
                .when(zoneRepoService).validateZoneName("");

        // 404 에러 확인
        assertThrows(BadRequestException.class, () -> zoneService.createZone(req));
    }

    // 2 - 1. updateZone 성공
    @Test
    void updateZone_success() {
        ZoneUpdateRequest dto = new ZoneUpdateRequest("수정된이름");
        Zone mockZone = new Zone("zone-1", "대기실");
        when(zoneRepoService.getZoneByName("대기실")).thenReturn(mockZone);
        when(zoneRepoService.save(any(Zone.class))).thenReturn(new Zone("zone-1", "수정된이름"));

        ZoneInfoResponse res = zoneService.updateZone("대기실", dto);

        assertThat(res.getZoneName()).isEqualTo("수정된이름");
        assertThat(res.getZoneId()).isEqualTo("zone-1");
    }
    // 2 - 2. updateZone 이미 존재하는 중복공간이 있을 경우 예외 발생
    @Test
    void updateZone_duplicateOtherName_throwsConflict() {
        Zone origin = zone();
        when(zoneRepoService.getZoneByName("대기실")).thenReturn(origin);

        // 이미 존재하는 이름이라서 validate 단계에서 예외 발생하도록
        doThrow(new DuplicateResourceException("중복"))
                .when(zoneRepoService).validateZoneName("포장구역");

        ZoneUpdateRequest dto = new ZoneUpdateRequest("포장구역");

        assertThrows(DuplicateResourceException.class,
                () -> zoneService.updateZone("대기실", dto));

        // validateZoneName 은 1회 호출되고 save() 는 호출되지 않아야 함
        verify(zoneRepoService).validateZoneName("포장구역");
        verify(zoneRepoService, never()).save(any());
    }

    /* ------------------------------------------------------------------
     * case ②  ( else 분기 ) : 새 이름이 기존 이름과 완전히 같음 → 즉시 예외
     * ------------------------------------------------------------------ */
    // 2-3. 공간 이름 수정을 하는데 이름이 변경된 점이 없을경우
    @Test
    void updateZone_sameName_throwsConflict() {
        Zone origin = zone();
        when(zoneRepoService.getZoneByName("대기실")).thenReturn(origin);

        ZoneUpdateRequest dto = new ZoneUpdateRequest("대기실"); // ← 기존 이름과 동일

        assertThrows(DuplicateResourceException.class,
                () -> zoneService.updateZone("대기실", dto));

        // 같은 이름이므로 validateZoneName() 호출조차 하지 않는다
        verify(zoneRepoService, never()).validateZoneName(anyString());
        verify(zoneRepoService, never()).save(any());
    }

    // 3. getAllZones
    /* 2. getAllZones_success ----------------------------------- */
    @Test
    void getAllZones_success() {
        when(zoneRepoService.findAll())
                .thenReturn(List.of(zone("zone-1","대기실"), zone("zone-2","A라인")));

        List<ZoneInfoResponse> res = zoneService.getAllZones();

        assertThat(res).extracting(ZoneInfoResponse::getZoneName)
                .containsExactlyInAnyOrder("대기실","A라인");
    }

    @Test
    void getAllZones_empty_throwsNotFound() {
        // given
        when(zoneRepoService.findAll()).thenReturn(Collections.emptyList());

        // when • then
        assertThrows(NotFoundException.class,
                () -> zoneService.getAllZones());

        // verify – 호출은 1번만
        verify(zoneRepoService).findAll();
    }

    @Test
    void getZoneItems_success() throws JsonProcessingException {
        Zone zone = new Zone("zone-1", "대기실");
        when(zoneRepoService.findAll()).thenReturn(List.of(zone));

        // 설비 mock 데이터 추가
        Equip facEquip = mock(Equip.class);
        when(facEquip.getEquipId()).thenReturn("equip-1");
        when(facEquip.getEquipName()).thenReturn("포장기");

        // ── 2. 이름이 "empty"인 설비 → 환경 센서가 들어오는 곳은 설비 이름이 empty가 됨
        Equip emptyEquip = mock(Equip.class);
        lenient().when(emptyEquip.getEquipId()).thenReturn("equip-2");
        when(emptyEquip.getEquipName()).thenReturn("empty"); // ❌ 필터링 대상

        // ── 3. 이름이 null인 설비 → 설비가 아얘 존재하지 않는 문제 구간
        Equip nullEquip = mock(Equip.class);
        lenient().when(nullEquip.getEquipId()).thenReturn("equip-3");
        when(nullEquip.getEquipName()).thenReturn(null);     // ❌ 필터링 대상

        // ── [환경] 센서가 참조할 더미 Equip  (equipId = zoneId)
        Equip envEquip = mock(Equip.class);
        when(envEquip.getEquipId()).thenReturn(zone.getZoneId());   // ★ zoneId == equipId
        lenient().when(envEquip.getEquipName()).thenReturn("ENV-DUMMY");

        // 환경센서를 위한 데이터
        Sensor envSensor = mock(Sensor.class);
        when(envSensor.getZone()).thenReturn(zone);
        when(envSensor.getEquip()).thenReturn(envEquip);
        when(envSensor.getSensorType()).thenReturn(SensorType.temp);
        when(envSensor.getSensorId()).thenReturn("S-1");

        // 공간에 존재하는 설비에 대한 설비 센서
        Sensor facSensor = mock(Sensor.class);
        when(facSensor.getZone()).thenReturn(zone);
        when(facSensor.getEquip()).thenReturn(facEquip);
        when(facSensor.getSensorType()).thenReturn(SensorType.vibration);
        when(facSensor.getSensorId()).thenReturn("S-2");

        when(sensorRepoService.findByZone(zone)).thenReturn(List.of(envSensor, facSensor));
        // 각 경우의 수에 모든 설비 데이터를 조회 요소로 넣어줌
        when(equipRepoService.findEquipsByZone(zone)).thenReturn(List.of(facEquip, emptyEquip, nullEquip));

        List<ZoneDetailResponse> res = zoneService.getZoneItems();

        // 객체 확인
        ObjectMapper om = new ObjectMapper();   // LocalDateTime 등 지원
        String resObj = om.writerWithDefaultPrettyPrinter()
                .writeValueAsString(res);

        System.out.println(resObj);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getZoneName()).isEqualTo("대기실");

        // 환경-센서 1개, 설비-센서 1개까지 확인하고 싶다면
        assertThat(res.get(0).getEnvList()).hasSize(1);
        assertThat(res.get(0).getEquipList()).hasSize(1);
        assertThat(res.get(0).getEquipList().get(0).getFacSensor()).hasSize(1);
    }

    @Test
    void findSystemLogsByZoneId_success() throws JsonProcessingException {
        String zoneId = "zone-1";
        Zone zone = new Zone(zoneId,"대기실");

        AbnormalPagingRequest paging = new AbnormalPagingRequest();
        paging.setPage(0);
        paging.setSize(10);

        // 이상치 로그 목 데이터
        AbnormalLog log = mock(AbnormalLog.class);
        when(log.getZone()).thenReturn(zone);
        when(log.getTargetType()).thenReturn(TargetType.Sensor);   // ★ 추가
        when(log.getAbnVal()).thenReturn(0.5);
        when(log.getAbnormalType()).thenReturn("TEMP");
        when(log.getDetectedAt()).thenReturn(LocalDateTime.now());

        Page<AbnormalLog> page = new PageImpl<>(List.of(log));
        when(abnormalLogRepoService
                .findByZone_ZoneIdOrderByDetectedAtDesc(eq(zoneId), any(Pageable.class)))
                .thenReturn(page);

        // 0번의 공간에 해당하는 10개의 페이징 불러오기
        Page<ZoneLogResponse> res = zoneService.findSystemLogsByZoneId(zoneId, paging);

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).getZoneId()).isEqualTo(zoneId);
        /* ── ① 검증 ───────────────────────────── */
        assertThat(res.getContent()).hasSize(1)
                .extracting(ZoneLogResponse::getZoneId)
                .containsExactly(zoneId);

        /* ── ② 예쁘게 출력 ────────────────────── */
        ObjectMapper om = new ObjectMapper()
                .findAndRegisterModules()               // LocalDateTime 직렬화 대비
                .enable(SerializationFeature.INDENT_OUTPUT);

        // 객체 확인
        String json = om.writeValueAsString(res.getContent());
        System.out.println("\n==== ZoneAbnLogResponse Pretty JSON ====\n" + json);
        System.out.println("==== end ====");
    }
}