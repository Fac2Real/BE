package com.factoreal.backend.messaging.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(title = "직접 입력한 메세지를 전송하기 위한 객체",description = "작업자와 메세지를 지정하여 요청합니다.")
@Data
public class FCMCustomRequest {
    String workerId;
    String message;
}
