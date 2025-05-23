package com.factoreal.backend.service;

import com.factoreal.backend.domain.worker.application.WorkerService;
import com.factoreal.backend.domain.worker.dto.response.WorkerInfoResponse;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.domain.zone.dao.ZoneHistoryRepository;
import com.factoreal.backend.domain.zone.entity.Zone;
import com.factoreal.backend.domain.zone.entity.ZoneHist;
import com.factoreal.backend.domain.worker.dao.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class WorkerServiceTest {

    @Mock
    private WorkerRepository workerRepository;
    
    @Mock
    private ZoneHistoryRepository zoneHistoryRepository;
    
    @InjectMocks
    private WorkerService workerService;

    private Worker worker1;
    private Worker worker2;
    private Zone zone1;
    private ZoneHist zoneHist1;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // 테스트용 작업자 데이터 생성
        worker1 = Worker.builder()
                .workerId("20240101-1234")
                .name("홍길동")
                .phoneNumber("01012345678")
                .email("hong@example.com")
                .build();
        
        worker2 = Worker.builder()
                .workerId("20240102-5678")
                .name("김철수")
                .phoneNumber("01087654321")
                .email("kim@example.com")
                .build();
                
        // 테스트용 공간 데이터 생성
        zone1 = Zone.builder()
                .zoneId("zone1")
                .zoneName("테스트 공간")
                .build();
                
        // 테스트용 ZoneHist 데이터 생성
        zoneHist1 = ZoneHist.builder()
                .id(1L)
                .worker(worker1)
                .zone(zone1)
                .startTime(LocalDateTime.now())
                .endTime(null)
                .existFlag(1)
                .build();
    }

    @Test
    public void testGetAllWorkers() {
        // Mock 설정
        when(workerRepository.findAll()).thenReturn(Arrays.asList(worker1, worker2));
        
        // 서비스 메소드 호출
        List<WorkerInfoResponse> result = workerService.getAllWorkers();
        
        // 결과 검증
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // 첫 번째 작업자 정보 확인
        assertEquals("20240101-1234", result.get(0).getWorkerId());
        assertEquals("홍길동", result.get(0).getName());
        assertEquals("01012345678", result.get(0).getPhoneNumber());
        assertEquals("hong@example.com", result.get(0).getEmail());
        assertEquals(false, result.get(0).getIsManager());  // 기본값은 false
        
        // 두 번째 작업자 정보 확인
        assertEquals("20240102-5678", result.get(1).getWorkerId());
        assertEquals("김철수", result.get(1).getName());
        assertEquals("01087654321", result.get(1).getPhoneNumber());
        assertEquals("kim@example.com", result.get(1).getEmail());
        assertEquals(false, result.get(1).getIsManager());  // 기본값은 false
    }

    @Test
    void testGetWorkersByZoneId() {
        // Mock 설정
        when(zoneHistoryRepository.findByZone_ZoneIdAndExistFlag("zone1", 1)).thenReturn(Arrays.asList(zoneHist1));
        
        // 테스트 실행
        List<WorkerInfoResponse> workers = workerService.getWorkersByZoneId("zone1");
        
        // 검증
        assertEquals(1, workers.size());
        assertEquals("20240101-1234", workers.get(0).getWorkerId());
        assertEquals("홍길동", workers.get(0).getName());
        assertEquals("01012345678", workers.get(0).getPhoneNumber());
        assertEquals("hong@example.com", workers.get(0).getEmail());
        assertEquals(false, workers.get(0).getIsManager());  // 관리자 여부는 더 이상 ZoneHist에서 확인할 수 없음
    }
}
