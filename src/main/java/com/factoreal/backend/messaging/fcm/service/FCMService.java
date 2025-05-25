package com.factoreal.backend.messaging.fcm.service;

import com.factoreal.backend.domain.worker.application.WorkerService;
import com.factoreal.backend.domain.worker.entity.Worker;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FCMService {
    private final FirebaseMessaging firebaseMessaging;
    private final WorkerService workerService;

    @Transactional
    public String saveToken(String workerId, String token) throws Exception {
        Worker worker = workerService.getWorkerByWorkerId(workerId);
        if (worker == null) {
            throw new Exception("Worker not found");
        }
        worker.setFcmToken(token);
        return workerService.saveWorker(worker).getFcmToken();
    }

    public void sendMessage(String token, String title, String body) throws Exception {
        String message = firebaseMessaging.send(
            Message.builder()
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .setToken(token)
                .build()
        );
        log.info("Message sent to firebase: {}",message);
    }
}
