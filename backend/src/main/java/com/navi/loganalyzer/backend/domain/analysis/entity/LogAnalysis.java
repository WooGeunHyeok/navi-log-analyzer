package com.navi.loganalyzer.backend.domain.analysis.entity;

import com.navi.loganalyzer.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TBL_LOG_ANALYSIS")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LogAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "FILE_ID", nullable = false)
    private Long fileId;

    // ParsingLog(TBL_PARSING_LOGS)의 ID 참조. PK를 공유하지 않고 fileId와 동일한 방식으로
    // 값만 들고 있는 참조 컬럼으로 둔다 - ParsingLog가 재파싱으로 삭제/재생성되어도
    // LogAnalysis 자신의 PK 채번에는 영향이 없다.
    @Column(name = "PARSING_LOG_ID", nullable = false)
    private Long parsingLogId;

    @Column(name = "STEP", nullable = false)
    private Long step;

    @Column(name = "TIMESTAMP")
    private String timestamp;

    @Column(name = "THREAD_ID", nullable = false)
    private Long threadId;

    @Column(name = "LOG_LEVEL")
    private String logLevel;

    @Column(name = "LAYER")
    private String layer;

    @Column(name = "RAW_MESSAGE", columnDefinition = "TEXT")
    private String rawMessage;

    // --- 소스코드 사전 매칭 정보 ---
    @Column(name = "FILE_NAME")
    private String fileName;

    @Column(name = "FILE_PATH")
    private String filePath;

    @Column(name = "FUNCTION_NAME")
    private String functionName;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "MATCH_TYPE")
    private String matchType;

    @Column(name = "DEPTH")
    private int depth;
}
