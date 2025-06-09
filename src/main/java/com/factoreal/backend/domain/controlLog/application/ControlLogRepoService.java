package com.factoreal.backend.domain.controlLog.application;

import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.factoreal.backend.domain.controlLog.dao.ControlLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ControlLogRepoService {

    private final ControlLogRepository controlLogRepository;

    public Map<Long, ControlLog> getControlLogs(List<Long> abnIds) {
        Map<Long, ControlLog> controlLogs = controlLogRepository
                .findByAbnormalLog_IdIn(abnIds)
                .stream()
                .collect(Collectors.toMap(
                        c -> c.getAbnormalLog().getId(),  // key: AbnormalLog ID
                        c -> c                            // value: ControlLog 객체
                ));
        return controlLogs;
    }

}
