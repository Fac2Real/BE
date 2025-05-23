package com.factoreal.backend.domain.abnormalLog.application;

import com.factoreal.backend.domain.abnormalLog.dto.LogType;
import com.factoreal.backend.domain.abnormalLog.dto.request.AbnormalPagingRequest;
import com.factoreal.backend.domain.abnormalLog.dto.response.AbnormalLogResponse;
import com.factoreal.backend.domain.sensor.dto.SensorKafkaDto;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.zone.dao.ZoneRepository;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.abnormalLog.dao.AbnLogRepository;
import com.factoreal.backend.messaging.kafka.strategy.alarmMessage.RiskMessageProvider;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.SensorType;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class AbnormalLogService {
    private final AbnLogRepository abnLogRepository;
    private final ZoneRepository zoneRepository;
    private final RiskMessageProvider riskMessageProvider;
    private final ObjectMapper objectMapper;

    // 알람 객체를 받아와서 로그 객체 생성.
    @Transactional(rollbackFor = Exception.class)
    public AbnormalLog saveAbnormalLogFromKafkaDto(
                                                        SensorKafkaDto sensorKafkaDto,
                                                        SensorType sensorType,
                                                        RiskLevel riskLevel,
                                                        LogType targetType
                                                ) throws Exception{


        Zone zone = zoneRepository.findByZoneId(sensorKafkaDto.getZoneId());


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
                .abnormalType(riskMessageProvider.getMessage(sensorType,riskLevel))
                .abnVal(sensorKafkaDto.getVal())
                .zone(zone)
                .detectedAt(LocalDateTime.parse(sensorKafkaDto.getTime()))
                .isRead(false)
                .build();

        return abnLogRepository.save(abnormalLog);
    }


    public Page<AbnormalLogResponse> findAllAbnormalLogs(AbnormalPagingRequest abnormalPagingDto) {
        // 한번에 DB전체를 주는 것이 아닌 구간 나눠서 전달하기 위함
        Pageable pageable = getPageable(abnormalPagingDto);
        Page<AbnormalLog> abnormalLogs = abnLogRepository.findAll(pageable);
        return abnormalLogs.map(abn_log ->
                AbnormalLogResponse.builder()
                        .id(abn_log.getId())
                        .targetType(abn_log.getTargetType())
                        .targetId(abn_log.getTargetId())
                        .abnormalType(abn_log.getAbnormalType())
                        .abnVal(abn_log.getAbnVal())
                        .detectedAt(abn_log.getDetectedAt())
                        .zoneId(abn_log.getZone().getZoneId())
                        .zoneName(abn_log.getZone().getZoneName())
                        .build()
        );
    }

    public Page<AbnormalLogResponse> findAllAbnormalLogsUnRead(AbnormalPagingRequest abnormalPagingRequest) {
        // 한번에 DB전체를 주는 것이 아닌 구간 나눠서 전달하기 위함
        Pageable pageable = getPageable(abnormalPagingRequest);
        Page<AbnormalLog> abnormalLogs = abnLogRepository.findAllByIsReadIsFalse(pageable);
        return abnormalLogs.map(abn_log ->
                AbnormalLogResponse.builder()
                        .id(abn_log.getId())
                        .targetType(abn_log.getTargetType())
                        .targetId(abn_log.getTargetId())
                        .abnormalType(abn_log.getAbnormalType())
                        .abnVal(abn_log.getAbnVal())
                        .detectedAt(abn_log.getDetectedAt())
                        .zoneId(abn_log.getZone().getZoneId())
                        .zoneName(abn_log.getZone().getZoneName())
                        .build()
        );
    }

    public Page<AbnormalLogResponse> findAbnormalLogsByAbnormalType(AbnormalPagingRequest abnormalPagingRequest, String abnormalType){
        // 한번에 DB전체를 주는 것이 아닌 구간 나눠서 전달하기 위함
        Pageable pageable = getPageable(abnormalPagingRequest);
        Page<AbnormalLog> abnormalLogs = abnLogRepository.findAbnormalLogsByAbnormalType(abnormalType,pageable);
        return abnormalLogs.map(abn_log ->
                AbnormalLogResponse.builder()
                        .id(abn_log.getId())
                        .targetType(abn_log.getTargetType())
                        .targetId(abn_log.getTargetId())
                        .abnormalType(abn_log.getAbnormalType())
                        .abnVal(abn_log.getAbnVal())
                        .detectedAt(abn_log.getDetectedAt())
                        .zoneId(abn_log.getZone().getZoneId())
                        .zoneName(abn_log.getZone().getZoneName())
                        .build()
        );
    }

    //
    public Page<AbnormalLogResponse> findAbnormalLogsByTargetId(AbnormalPagingRequest abnormalPagingRequest, String targetType, String targetId){
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
