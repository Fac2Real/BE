package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dao.AbnLogRepository;
import com.factoreal.backend.domain.abnormalLog.dto.TargetType;
import com.factoreal.backend.domain.abnormalLog.dto.request.AbnormalPagingRequest;
import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.zone.application.ZoneHistoryService;
import com.factoreal.backend.domain.zone.application.ZoneService;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
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
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class AbnormalLogService {
    private final AbnLogRepository abnLogRepository;
    private final ZoneService zoneService;
    private final RiskMessageProvider riskMessageProvider;
    private final ObjectMapper objectMapper;
    private final ZoneHistoryService zoneHistoryService;

    /**
     * 센서 데이터 기반의 알람 로그 생성.
     * @param sensorKafkaDto kafka에서 EQUIPMENT 및 ENVIRONMENT 토픽으로 들어오는 DTO
     * @param sensorType 센서 종류: current, dust, temp, humid, vibration, voc
     * @param riskLevel 위험 레벨: 시스템 로그에 저장할 메세지를 조회하기 위해 필요(센서별 위험도에 해당되는 메세지)
     * @param targetType 타겟 종류 : Sensor(공간), Worker(작업자), Equip(설비-머신러닝)
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public AbnormalLog saveAbnormalLogFromSensorKafkaDto(
            SensorKafkaDto sensorKafkaDto,
            SensorType sensorType,
            RiskLevel riskLevel,
            TargetType targetType
    ) throws Exception{
        Zone zone = zoneService.findByZoneId(sensorKafkaDto.getZoneId());

        if (zone == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 공간 ID: " + sensorKafkaDto.getZoneId());
        }

        log.info(">>>>>> zone : {} " ,zone);

        // DTO의 severity (AlarmEvent.RiskLevel)를 Entity RiskLevel로 매핑
//        RiskLevel entityRiskLevel = mapDtoSeverityToEntityRiskLevel(riskLevel);
        // [TODO] 현재는 스프린트 1 웹 푸쉬, 대시보드 히트 맵 알림 로그만 구현되있음. worker, equip 로그용 구현 필요.
        AbnormalLog abnormalLog = AbnormalLog.builder()
                .targetId(sensorKafkaDto.getSensorId())
                .targetType(targetType)
                .abnormalType(riskMessageProvider.getRiskMessageBySensor(sensorType,riskLevel))
                .abnVal(sensorKafkaDto.getVal())
                .dangerLevel(riskLevel.getPriority())
                .zone(zone)
                .detectedAt(LocalDateTime.parse(sensorKafkaDto.getTime()))
                .isRead(false)
                .build();

        return abnLogRepository.save(abnormalLog);
    }

    /**
     * 센서 데이터 기반의 알람 로그 생성.
     * @param wearableKafkaDto kafka에서 WEARABLE 토픽으로 들어오는 DTO
     * @param wearableDataType 생체 데이터 종류: 현재는 heartRate 만 보내는 중(확장성 고려해서 해당 객체 사용)
     * @param riskLevel 위험 레벨: 시스템 로그에 저장할 메세지를 조회하기 위해 필요(생체 데이터 별 위험도에 해당되는 메세지)
     * @param targetType 타겟 종류 : Sensor(공간), Worker(작업자), Equip(설비-머신러닝)
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public AbnormalLog saveAbnormalLogFromWearableKafkaDto(
            WearableKafkaDto wearableKafkaDto,
            WearableDataType wearableDataType,
            RiskLevel riskLevel,
            TargetType targetType
    ){
        // workerId에 해당되는 사람이 제일 최근에 있던 공간 조회
        ZoneHist zonehist = zoneHistoryService.
                findByWorker_WorkerIdAndExistFlag(wearableKafkaDto.getWorkerId(), 1);
        Zone zone;
        if (zonehist == null){
            zone = zoneService.findByZoneId("00000000000000-000");
        }else{
            zone = zonehist.getZone();
        }

        AbnormalLog abnormalLog = AbnormalLog.builder()
                .targetId(wearableKafkaDto.getWearableDeviceId())
                .targetType(targetType)
                .abnormalType(riskMessageProvider.getRiskMessageByWearble(wearableDataType,riskLevel))
                .abnVal(Double.valueOf(wearableKafkaDto.getVal()))
                .detectedAt(LocalDateTime.parse(wearableKafkaDto.getTime()))
                .dangerLevel(riskLevel.getPriority())
                .zone(zone)
                .isRead(false)
                .build();
       return abnLogRepository.save(abnormalLog);
    }

    public Page<AbnormalLogResponse> findAllAbnormalLogs(AbnormalPagingRequest abnormalPagingDto) {
        // 한번에 DB전체를 주는 것이 아닌 구간 나눠서 전달하기 위함
        Pageable pageable = getPageable(abnormalPagingDto);
        Page<AbnormalLog> abnormalLogs = abnLogRepository.findAll(pageable);
        return abnormalLogs.map(AbnormalLog::fromEntity);
    }

    public Page<AbnormalLogResponse> findAllAbnormalLogsUnRead(AbnormalPagingRequest abnormalPagingRequest) {
        // 한번에 DB전체를 주는 것이 아닌 구간 나눠서 전달하기 위함
        Pageable pageable = getPageable(abnormalPagingRequest);
        Page<AbnormalLog> abnormalLogs = abnLogRepository.findAllByIsReadIsFalseOrderByDetectedAtDesc(pageable);
        return abnormalLogs.map(AbnormalLog::fromEntity);
    }

    public Page<AbnormalLogResponse> findAbnormalLogsByAbnormalType(AbnormalPagingRequest abnormalPagingRequest, String abnormalType){
        // 한번에 DB전체를 주는 것이 아닌 구간 나눠서 전달하기 위함
        Pageable pageable = getPageable(abnormalPagingRequest);
        Page<AbnormalLog> abnormalLogs = abnLogRepository.findAbnormalLogsByAbnormalType(abnormalType,pageable);
        return abnormalLogs.map(AbnormalLog::fromEntity);
    }

    public List<AbnormalLogResponse> findLatestAbnormalLogsForTargets(TargetType targetType, List<String> targetIds) {
        return targetIds.stream()
            .map(targetId ->
                abnLogRepository.findFirstByTargetTypeAndTargetIdOrderByDetectedAtDesc(targetType, targetId)
                    .map(AbnormalLog::fromEntity) // 또는 objectMapper.convertValue(...)
                    .orElse(null) // 값이 없으면 null
            )
            .filter(Objects::nonNull) // null 제거
            .toList();
    }
    public Page<AbnormalLogResponse> findAbnormalLogsByTargetId(AbnormalPagingRequest abnormalPagingRequest, TargetType targetType, String targetId){
        // 한번에 DB전체를 주는 것이 아닌 구간 나눠서 전달하기 위함
        Pageable pageable = getPageable(abnormalPagingRequest);
        Page<AbnormalLog> abnormalLogs = abnLogRepository.findAbnormalLogsByTargetTypeAndTargetId(
                targetType,
                targetId,
                pageable);
        return abnormalLogs.map(
                abn_log -> objectMapper.convertValue(abn_log, AbnormalLogResponse.class)
        );
    }


    // FE에서 알람을 클릭한 경우 읽음으로 수정
    @Transactional
    public boolean readCheck(Long abnormalLogId){
        AbnormalLog abnormalLog = abnLogRepository.findById(abnormalLogId).orElse(null);
        if(abnormalLog == null){
            return false;
        }

        abnormalLog.setIsRead(true);
        abnLogRepository.save(abnormalLog);
        readRequired();
        return true;
    }
    @Transactional(readOnly = true)
    // 읽지 않은 알람이 몇개인지 반환
    public Long readRequired(){
        Long count =  abnLogRepository.countByIsReadFalse();
//        webSocketSender.sendUnreadCount(count);
        return count;
    }

    private Pageable getPageable(AbnormalPagingRequest abnormalPagingRequest){
        return PageRequest.of(
                abnormalPagingRequest.getPage(),
                abnormalPagingRequest.getSize()
        );
    }
}
