package com.factoreal.backend.messaging.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker // STOMP 기반 websocket 메시징 활성화.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 연결할 엔드포인트
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 구독 주소
        config.enableSimpleBroker("/topic");
        // 메시지 보낼 prefix (ex: @SendTo("/topic/xxx"))
        config.setApplicationDestinationPrefixes("/app");
    }
}
