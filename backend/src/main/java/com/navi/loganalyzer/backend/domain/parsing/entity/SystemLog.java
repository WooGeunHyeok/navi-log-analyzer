package com.navi.loganalyzer.backend.domain.parsing.entity;

import com.navi.loganalyzer.backend.domain.logupload.entity.LogFileUpload;
import com.navi.loganalyzer.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@Table(name = "TBL_SYSTEM_LOGS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SystemLog extends BaseEntity {

    /**
     * Container/JNI_Layer/Java_Layer가 아닌 로그 중, 사전 조건 판별에 필요하다고 등록된
     * 키워드(TBL_SYSTEM_LOG_KEYWORD)에 매칭된 줄만 저장하는 엔티티.
     * 흐름도 트리(TBL_PARSING_LOGS)와 달리 실행 순서(step)나 depth 개념이 없는 평평한 목록이다.
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOG_FILE_ID", nullable = false)
    private LogFileUpload logFile;

    @Column(name = "TIMESTAMP", nullable = false, length = 30)
    private String timestamp;

    @Column(name = "THREAD_ID", nullable = false)
    private Long threadId;

    @Column(name = "LOG_LEVEL", length = 10)
    private String logLevel;

    @Column(name = "TAG", length = 100)
    private String tag;

    @Column(name = "RAW_MESSAGE", columnDefinition = "TEXT")
    private String rawMessage;
}
