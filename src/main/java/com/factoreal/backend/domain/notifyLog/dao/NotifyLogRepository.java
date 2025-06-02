package com.factoreal.backend.domain.notifyLog.dao;

import com.factoreal.backend.domain.notifyLog.entity.NotifyLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotifyLogRepository extends JpaRepository<NotifyLog,Integer> {
}
