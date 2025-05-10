package com.factoreal.backend.strategy;

import com.factoreal.backend.strategy.enums.AlarmEvent;
import com.factoreal.backend.entity.Worker;
import com.factoreal.backend.repository.WorkerRepository;
import com.factoreal.backend.strategy.enums.AlarmType;
import com.factoreal.backend.strategy.enums.RiskLevel;
import com.factoreal.backend.strategy.enums.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@Testcontainers
class SmsNotificationStrategyTest {
    @Container
    static LocalStackContainer localstack = new LocalStackContainer("latest")
            .withServices(LocalStackContainer.Service.SNS);

    private SmsNotificationStrategy smsNotificationStrategy;
    private WorkerRepository workerRepository;
    private SnsClient snsClient;
    private AlarmEvent alarmEvent;
    @BeforeEach
    void setUp() {
        // LocalStack SNS 클라이언트 생성
        snsClient = SnsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SNS))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                        )
                )
                .region(Region.of(localstack.getRegion()))
                .build();
        alarmEvent = new AlarmEvent(
                UUID.randomUUID(),
                SensorType.humid.name(),
                29.0f,
                RiskLevel.WARNING,
                Timestamp.valueOf(LocalDateTime.now()),
                AlarmType.LOW_HUMIDITY.getMessage(),
                "Sensor"
                );
        // Mock Repository
        workerRepository = mock(WorkerRepository.class);

        // 전략 객체 생성
        smsNotificationStrategy = new SmsNotificationStrategy(snsClient, workerRepository);
    }

    @Test
    void send_sms_successfully() {
        // given
        Worker worker = new Worker();
        worker.setPhoneNumber("+821012341234"); // SNS 포맷
        when(workerRepository.findById("user123")).thenReturn(Optional.of(worker));

        // when
        smsNotificationStrategy.send(alarmEvent);

        // then
        verify(workerRepository, times(1)).findById("user123");
    }

    @Test
    void send_sms_worker_not_found() {
        when(workerRepository.findById("not_found")).thenReturn(Optional.empty());
        smsNotificationStrategy.send(alarmEvent);
        verify(workerRepository, times(1)).findById("not_found");
    }
}