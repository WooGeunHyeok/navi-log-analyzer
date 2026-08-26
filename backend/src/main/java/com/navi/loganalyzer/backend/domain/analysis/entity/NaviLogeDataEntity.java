package com.navi.loganalyzer.backend.domain.analysis.entity;

import com.navi.loganalyzer.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "TBL_LOG_DICTIONARY")
@Getter

public class NaviLogeDataEntity extends BaseEntity {

    /**
     * 사전 DB Entity - Parsing DB와 매핑하기 위해
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "FILE_NAME", nullable = false)
    private String fileName;

    @Column(name = "FILE_PATH", nullable = false)
    private String filePath;

    @Column(name = "FUNCTION_NAME", nullable = false)
    private String functionName;

    @Column(name = "LINE_NUMBER", nullable = false)
    private Integer lineNumber;

    @Column(name = "LOG_TYPE", nullable = false)
    private String logType;

    @Column(name = "RAW_MESSAGE", columnDefinition = "TEXT")
    private String rawMessage;

    @Column(name = "PATTERN_MESSAGE", columnDefinition = "TEXT")
    private String patternMessage;

    @Column(name = "calls", columnDefinition = "TEXT")
    private String calls;
}
