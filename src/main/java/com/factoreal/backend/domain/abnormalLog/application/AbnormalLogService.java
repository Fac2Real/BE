package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.request.AbnormalPagingRequest;
import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.equip.entity.Equip;
import com.factoreal.backend.domain.sensor.application.SensorRepoService;
import com.factoreal.backend.domain.sensor.dao.SensorRepository;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.sensor.entity.Sensor;
import com.factoreal.backend.domain.state.store.InMemoryZoneWorkerStateStore;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.messaging.kafka.dto.WearableKafkaDto;
import com.factoreal.backend.messaging.kafka.strategy.alarmMessage.RiskMessageProvider;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AbnormalLogService {
    private final AbnormalLogRepoService abnormalLogRepoService;
    private final RiskMessageProvider riskMessageProvider;
    private final ObjectMapper objectMapper;
    private final ZoneRepoService zoneRepoService;
    private final InMemoryZoneWorkerStateStore zoneWorkerStateStore;
    private final SensorRepository sensorRepository;
    private final SensorRepoService sensorRepoService;

    /**
     * 센서 데이터 기반의 알람 로그 생성.
     *
     * @param sensorKafkaDto kafka에서 EQUIPMENT 및 ENVIRONMENT 토픽으로 들어오는 DTO
     * @param sensorType     센서 종류: current, dust, temp, humid, vibration, voc
     * @param riskLevel      위험 레벨: 시스템 로그에 저장할 메세지를 조회하기 위해 필요(센서별 위험도에 해당되는 메세지)
     * @param targetType     타겟 종류 : Sensor(공간), Worker(작업자), Equip(설비-머신러닝)
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)

    public AbnormalLog saveAbnormalLogFromSensorKafkaDto(
            SensorKafkaDto sensorKafkaDto,
            SensorType sensorType,
            RiskLevel riskLevel,
            TargetType targetType
    ) throws Exception {
        Zone zone = zoneRepoService.findById(sensorKafkaDto.getZoneId());

        if (zone == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 공간 ID: " + sensorKafkaDto.getZoneId());
        }

        log.info(">>>>>> zone : {} ", zone);

        // DTO의 severity (AlarmEvent.RiskLevel)를 Entity RiskLevel로 매핑
        // RiskLevel entityRiskLevel = mapDtoSeverityToEntityRiskLevel(riskLevel);
        // [TODO] 현재는 스프린트 1 웹 푸쉬, 대시보드 히트 맵 알림 로그만 구현되있음. worker, equip 로그용 구현 필요.
        AbnormalLog abnormalLog = AbnormalLog.builder()
                .targetId(sensorKafkaDto.getSensorId())
                .targetType(targetType)
                .abnormalType(riskMessageProvider.getRiskMessageBySensor(sensorType, riskLevel))
                .abnVal(sensorKafkaDto.getVal())
                .dangerLevel(riskLevel.getPriority())
                .zone(zone)
                .detectedAt(LocalDateTime.parse(sensorKafkaDto.getTime()))
                .isRead(false)
                .build();

        return abnormalLogRepoService.save(abnormalLog);
    }

    /**
     * 센서 데이터 기반의 알람 로그 생성.
     *
     * @param wearableKafkaDto kafka에서 WEARABLE 토픽으로 들어오는 DTO
     * @param wearableDataType 생체 데이터 종류: 현재는 heartRate 만 보내는 중(확장성 고려해서 해당 객체 사용)
     * @param riskLevel        위험 레벨: 시스템 로그에 저장할 메세지를 조회하기 위해 필요(생체 데이터 별 위험도에 해당되는 메세지)
     * @param targetType       타겟 종류 : Sensor(공간), Worker(작업자), Equip(설비-머신러닝)
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public AbnormalLog saveAbnormalLogFromWearableKafkaDto(
            WearableKafkaDto wearableKafkaDto,
            WearableDataType wearableDataType,
            RiskLevel riskLevel,
            TargetType targetType
    ) {
        // workerId에 해당되는 사람이 제일 최근에 있던 공간 조회
        Zone zone = zoneRepoService.findById(zoneWorkerStateStore.getZoneId(wearableKafkaDto.getWorkerId()));

        AbnormalLog abnormalLog = AbnormalLog.builder()
                .targetId(wearableKafkaDto.getWorkerId())
                .targetType(targetType)
                .abnormalType(riskMessageProvider.getRiskMessageByWearble(wearableDataType, riskLevel))
                .abnVal(Double.valueOf(wearableKafkaDto.getVal()))
                .detectedAt(LocalDateTime.parse(wearableKafkaDto.getTime()))
                .dangerLevel(riskLevel.getPriority())
                .zone(zone)
                .isRead(false)
                .build();
        return abnormalLogRepoService.save(abnormalLog);
    }

    public Page<AbnormalLogResponse> findAllAbnormalLogs(AbnormalPagingRequest abnormalPagingDto) {
        // 한번에 DB전체를 주는 것이 아닌 구간 나눠서 전달하기 위함
        Pageable pageable = getPageable(abnormalPagingDto);
        Page<AbnormalLog> abnormalLogs = abnormalLogRepoService.findAll(pageable);
        return abnormalLogs.map(AbnormalLog::fromEntity);
    }

    public Page<AbnormalLogResponse> findAllAbnormalLogsUnRead(AbnormalPagingRequest abnormalPagingRequest) {
        // 한번에 DB전체를 주는 것이 아닌 구간 나눠서 전달하기 위함
        Pageable pageable = getPageable(abnormalPagingRequest);
        Page<AbnormalLog> abnormalLogs = abnormalLogRepoService.findAllByIsReadIsFalseOrderByDetectedAtDesc(pageable);
        return abnormalLogs.map(AbnormalLog::fromEntity);
    }

    public Page<AbnormalLogResponse> findAbnormalLogsByAbnormalType(AbnormalPagingRequest abnormalPagingRequest,
                                                                    String abnormalType) {
        // 한번에 DB전체를 주는 것이 아닌 구간 나눠서 전달하기 위함
        Pageable pageable = getPageable(abnormalPagingRequest);
        Page<AbnormalLog> abnormalLogs = abnormalLogRepoService.findAbnormalLogsByAbnormalType(pageable, abnormalType);
        return abnormalLogs.map(AbnormalLog::fromEntity);
    }

    public List<AbnormalLogResponse> findLatestAbnormalLogsForTargets(TargetType targetType, List<String> targetIds) {
        log.info("findLatestAbnormalLogsForTargets 호출 - targetType: {}, targetIds: {}", targetType, targetIds);
        List<AbnormalLogResponse> responses = targetIds.stream()
                .map(targetId -> {
                    Optional<AbnormalLog> latestLog = abnormalLogRepoService.findFirstByTargetTypeAndTargetIdOrderByDetectedAtDesc(targetType, targetId);
                    log.info("작업자 ID: {} 의 최신 로그: {}", targetId, latestLog.orElse(null));
                    return latestLog
                            .map(AbnormalLogResponse::from)
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();
        log.info("최종 반환되는 응답 목록: {}", responses);
        return responses;
    }

    public Page<AbnormalLogResponse> findAbnormalLogsByTargetId(AbnormalPagingRequest abnormalPagingRequest, TargetType targetType, String targetId) {
        // 한번에 DB전체를 주는 것이 아닌 구간 나눠서 전달하기 위함
        Pageable pageable = getPageable(abnormalPagingRequest);
        Page<AbnormalLog> abnormalLogs = abnormalLogRepoService.findAbnormalLogsByTargetTypeAndTargetId(
                pageable,
                targetType,
                targetId);
        return abnormalLogs.map(
                abn_log -> objectMapper.convertValue(abn_log, AbnormalLogResponse.class));
    }

    // FE에서 알람을 클릭한 경우 읽음으로 수정
    @Transactional
    public boolean readCheck(Long abnormalLogId) {
        AbnormalLog abnormalLog = abnormalLogRepoService.findById(abnormalLogId);
        if (abnormalLog == null) {
            return false;
        }

        abnormalLog.setIsRead(true);
        abnormalLogRepoService.save(abnormalLog);
        return true;
    }

    // 센서 abnormal log 저장
    @Transactional
    public AbnormalLog saveAbnormalLog(SensorKafkaDto dto, Sensor sensor, int dangerLevel) {
        RiskLevel riskLevel = RiskLevel.fromPriority(dangerLevel);

        AbnormalLog abnormalLog = AbnormalLog.builder()
                .targetId(dto.getSensorId())
                .targetType(TargetType.Sensor)
                .abnormalType(riskMessageProvider.getRiskMessageBySensor(sensor.getSensorType(), riskLevel))
                .abnVal(dto.getVal())
                .zone(sensor.getZone())
                .detectedAt(LocalDateTime.parse(dto.getTime()))
                .isRead(false)
                .build();

        return abnormalLogRepoService.save(abnormalLog);
    }

    private Pageable getPageable(AbnormalPagingRequest abnormalPagingRequest) {
        return PageRequest.of(
                abnormalPagingRequest.getPage(),
                abnormalPagingRequest.getSize());
    }

    /**
     * 설비 예측 기반 이상 로그 저장
     * @param zone 공간 엔티티
     * @param equip 설비 엔티티
     * @param remainDays 계산된 잔존 수명(일)
     * @param dangerLevel 위험 레벨 (1 또는 2)
     */
    @Transactional
    public void saveEquipAbnormalLog(Zone zone, Equip equip, int remainDays, int dangerLevel) {
        AbnormalLog abnormalLog = AbnormalLog.builder()
                .targetType(TargetType.Equip)
                .targetId(equip.getEquipId())
                .abnormalType("설비 잔존 수명 경고")      // 필요에 따라 상세 메시지로 변경
                .abnVal((double) remainDays)
                .dangerLevel(dangerLevel)
                .zone(zone)
                .detectedAt(LocalDateTime.now())
                .isRead(false)
                .build();

        abnormalLogRepoService.save(abnormalLog);
        log.info("📝 설비 이상 로그 저장 (equipId={}, dangerLevel={})", equip.getEquipId(), dangerLevel);
    }


}
