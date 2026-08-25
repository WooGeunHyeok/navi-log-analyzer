package com.navi.loganalyzer.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * DB 연결 및 JPA Auditing (@EnableJpaAuditing)을 활성화하여 로그 생성/분석 기록 시간을 자동 생성
     */

    // JPA Auditing 활성화 (@CreadteDate, @LastModifieDate 자동 기록)
}
