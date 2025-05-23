package com.factoreal.backend.messaging.kafka.strategy.alarmList;

import com.factoreal.backend.messaging.kafka.strategy.enums.AlarmEventDto;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import com.factoreal.backend.domain.worker.dao.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Optional;

@Slf4j
@Component("SMS")
@RequiredArgsConstructor
public class SmsNotificationStrategy implements NotificationStrategy {
    // AWS SNS 서비스 사용을 위한 객체
    private final SnsClient snsClient;
    private final WorkerRepository workerRepository;

    private static final String userId = "alarm-test";
    // TODO Seoul 리전에 SMS 지원이 안되기에 Slack으로 변경
    @Override
    public void send(AlarmEventDto alarmEventDto) {
        log.info("📬 SMS Notification Strategy.");
        // Todo 공간에 있는 작업자 목록 가져오기
        // Wearable 앱이 선행되어야함...
        // 공간 정보는 KafkaConsumer에서 만든 alarmEvent 객체에 있음.
        Optional<Worker> workerOptional = workerRepository.findById(userId);
        if  (workerOptional.isEmpty()) {
            log.error("❌해당 아이디를 가진 직원이 없습니다.{}", userId);
            return;
        }
        try{
            PublishRequest publishRequest = PublishRequest.builder()
                    .message(alarmEventDto.getMessageBody())
                    .phoneNumber(workerOptional.get().getPhoneNumber()) // 형식 (국가번호)전화번호 => +82 10-1234-1234
                    .build();

            PublishResponse publishResponse = snsClient.publish(publishRequest);
            log.info("✅ SMS Publish Response: {}", publishResponse);
        }catch (Exception e){
            log.error("❌ SMS Publish Exception: {}", e.getMessage());
        }
    }

    @Override
    public RiskLevel getSupportedLevel() {
        return RiskLevel.CRITICAL;
    }
}
