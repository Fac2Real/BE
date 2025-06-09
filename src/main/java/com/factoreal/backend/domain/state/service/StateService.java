package com.factoreal.backend.domain.state.service;

import com.factoreal.backend.domain.state.store.ZoneSensorStateStore;
import com.factoreal.backend.domain.state.store.ZoneWorkerStateStore;
import com.factoreal.backend.domain.zone.application.ZoneRepoService;
import com.factoreal.backend.messaging.common.dto.ZoneDangerDto;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.messaging.kafka.strategy.enums.WearableDataType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StateService {
    private final ZoneRepoService zoneRepoService;
    private final ZoneSensorStateStore zoneSensorStateStore;
    private final ZoneWorkerStateStore zoneWorkerStateStore;

    public List<ZoneDangerDto> getAllZoneStates() {
        return zoneRepoService.findAll().stream()
                .map(zone -> {
                    String zoneId = zone.getZoneId();
                    RiskLevel zoneSensorLevel = zoneSensorStateStore.getZoneRiskLevel(zoneId);
                    RiskLevel zoneWorkerLevel = zoneWorkerStateStore.getZoneRiskLevel(zoneId);

                    ZoneDangerDto zoneDangerDto = new ZoneDangerDto();
                    if (zoneSensorLevel == RiskLevel.INFO && zoneWorkerLevel == RiskLevel.INFO) {
                        return null;
                    } else if (zoneSensorLevel.getPriority() >= zoneWorkerLevel.getPriority()) {
                        zoneDangerDto.setZoneId(zoneId);
                        zoneDangerDto.setLevel(zoneSensorLevel.getPriority());
                        zoneDangerDto.setSensorType(zoneSensorStateStore.getHighestRiskSensor(zoneId).getSensorType().name());
                        return zoneDangerDto;
                    } else {
                        zoneDangerDto.setZoneId(zoneId);
                        zoneDangerDto.setLevel(zoneWorkerLevel.getPriority());
                        zoneDangerDto.setSensorType(WearableDataType.heartRate.name());
                        return zoneDangerDto;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
