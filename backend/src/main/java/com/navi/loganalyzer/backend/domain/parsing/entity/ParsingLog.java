package com.navi.loganalyzer.backend.domain.parsing.entity;


import com.navi.loganalyzer.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.navi.loganalyzer.backend.domain.log.entity.LogFileUpload;

@Entity
@Getter
@Table(name = "TBL_PARSING_LOGS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParsingLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TBL_LOGS_FILES", nullable = false)
    private LogFileUpload logFile;

    @Column(name = "STEP", nullable = false)
    private Long step;                                      // 로그 내 순체적 실행 순서 (1, 2, 3...)

    @Column(name = "TIMESTAMP", nullable = false, length = 30)
    private String timestamp;                               // "01-01 15:23:17.870"

    @Column(name = "THREAD_ID", nullable = false)
    private Long threadId;                                  // TID (스레드별 흐름 분리용)

    @Column(name = "LOG_LEVEL", length = 10)
    private String logLevel;                                // E, W, I, D 등

    @Enumerated(EnumType.STRING)
    @Column(name = "LAYER", nullable = false, length = 20)
    private LogLayer layer;                                 // CONTAINER, JNI_LAYER, JAVA_LAYER

    @Column(name = "FUNCTION_NAME", length = 100)
    private String functionName;                            // DB 매핑용 Key 1 (함수명)

    @Column(name = "LINE_NUM")
    private Long lineNum;                                   // DB 매핑용 Key 2 (줄 번호, null 허용)

    @Column(name = "RAW_MESSAGE", columnDefinition = "TEXT")
    private String rawMessage;                              // 원본 로그 메시지

    @Column(name = "DEPTH")
    private Long depth;                                     // TID 기반으로 계산된 호출 깊이 (0, 1, 2...)

    @Builder
    public ParsingLog(LogFileUpload logFile, Long step, String timestamp, Long threadId, String logLevel, LogLayer layer,
                      String functionName, Long lineNum, String rawMessage, Long depth) {
        this.logFile = logFile;
        this.step = step;
        this.timestamp = timestamp;
        this.threadId = threadId;
        this.logLevel = logLevel;
        this.layer = layer;
        this.functionName = functionName;
        this.lineNum = lineNum;
        this.rawMessage = rawMessage;
        this.depth = depth;
    }
}
