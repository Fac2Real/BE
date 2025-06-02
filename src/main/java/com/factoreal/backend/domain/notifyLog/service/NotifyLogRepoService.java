package com.factoreal.backend.domain.notifyLog.service;

import com.factoreal.backend.domain.notifyLog.dao.NotifyLogRepository;
import com.factoreal.backend.domain.notifyLog.entity.NotifyLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotifyLogRepoService {
    private final NotifyLogRepository notifyLogRepository;

    @Transactional
    public NotifyLog saveNotifyLog(NotifyLog notifyLog) {
        return notifyLogRepository.save(notifyLog);
    }
}
