package com.factoreal.backend.messaging.fcm.api;

import com.factoreal.backend.messaging.fcm.dto.FCMTokenRegistDto;
import com.factoreal.backend.messaging.fcm.service.FCMService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fcm")
@Tag(name = "모바일 앱 FCm 등록용 API",description = "모바일 앱에서 FCM 수신용 토큰을 등록하는 API")
@Slf4j
@RequiredArgsConstructor
public class FCMController {
    private final FCMService fcmService;
    @PostMapping
    public ResponseEntity<String> sendMessage(@RequestBody FCMTokenRegistDto message) {
        try{
            String response = fcmService.saveToken(message.getWorkerId(),message.getToken());
            if(response.equals(message.getToken())){
                return ResponseEntity.ok().body(response);
            }
            return ResponseEntity.badRequest().body(response);
        }catch(Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
