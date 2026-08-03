package com.navi.loganalyzer.backend.global.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 내비게이션 로그 파일을 비동기로 파싱/분석할 때 서버가 멈추지 않도록 스레드 풀을 관리
     */

    @Bean(name = "logParsingExecutor")
    public Executor logParsingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("LogParser-");
        executor.initialize();
        return executor;
    }
}
