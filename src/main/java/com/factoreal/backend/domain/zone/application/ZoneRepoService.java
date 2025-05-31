package com.factoreal.backend.domain.zone.application;

import com.factoreal.backend.domain.zone.dao.ZoneRepository;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.global.exception.dto.BadRequestException;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ZoneRepoService {

    private final ZoneRepository zoneRepository;

    /**
     * 모든 zone을 조회하는 레포지토리 접근 서비스
     */
    public List<Zone> findAll() {
        return zoneRepository.findAll();
    }

    /**
     * 공간 이름으로 공간 조회
     */
    public Zone getZoneByName(String zoneName) {
        return zoneRepository.findByZoneName(zoneName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 공간: " + zoneName));
    }

    /**
     * 존 데이터 저장하는 레포지토리 접근 서비스
     */
    @Transactional
    public Zone save(Zone zone) {
        return zoneRepository.save(zone);
    }

    /**
     * 이름으로 공간이 존재하는지 체크
     */
    public void validateZoneName(String zoneName) {
        if (zoneRepository.findByZoneName(zoneName).isPresent()) {
            throw new BadRequestException("이미 존재하는 공간명: " + zoneName);
        }
    }

    public Zone findById(String zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("공간을 찾을 수 없습니다: " + zoneId));
    }

    public Zone findByZoneName(String zoneName) {
        return zoneRepository.findByZoneName(zoneName)
                .orElseThrow(() -> new BadRequestException("존재하지 않는 공간명: " + zoneName));
    }

    public Zone findByZoneId(String zoneId) {
        return zoneRepository.findByZoneId(zoneId);
    }
}
