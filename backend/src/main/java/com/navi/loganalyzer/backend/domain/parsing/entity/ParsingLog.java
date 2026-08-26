package com.navi.loganalyzer.backend.domain.parsing.entity;


import com.navi.loganalyzer.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import com.navi.loganalyzer.backend.domain.logupload.entity.LogFileUpload;

@Entity
@Getter
@Builder
@Table(name = "TBL_PARSING_LOGS")
@AllArgsConstructor
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
    private Long step;

    @Column(name = "TIMESTAMP", nullable = false, length = 30)
    private String timestamp;

    @Column(name = "THREAD_ID", nullable = false)
    private Long threadId;

    @Column(name = "LOG_LEVEL", length = 10)
    private String logLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "LAYER", nullable = false, length = 20)
    private LogLayer layer;

    @Column(name = "FUNCTION_NAME", length = 100)
    private String functionName;

    @Column(name = "LINE_NUM")
    private Integer lineNum;

    @Column(name = "RAW_MESSAGE", columnDefinition = "TEXT")
    private String rawMessage;
}
