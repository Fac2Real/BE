package com.factoreal.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync   // <-- @Async를 사용할 수 있도록 활성화
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "websocketExecutor")
    public Executor websocketExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // 동시에 실행할 기본 스레드 개수
        executor.setMaxPoolSize(20);      // 최대 스레드 개수
        executor.setQueueCapacity(100);   // 대기 큐 크기
        executor.setThreadNamePrefix("ws-async-");
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        // @Async가 별도 executor 이름을 지정하지 않으면 이게 디폴트로 사용됩니다.
        return websocketExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // @Async 메서드 내 예외를 처리할 핸들러
        return (throwable, method, params) -> {
            // 로깅 또는 알림 등
            System.err.printf("Async error in method %s: %s%n", method.getName(), throwable.getMessage());
        };
    }
}