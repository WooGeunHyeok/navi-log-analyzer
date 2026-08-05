package com.navi.loganalyzer.backend.domain.log.entity;

import com.navi.loganalyzer.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TBL_LOGS_FILES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LogFileUpload extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;                       // 로그 제목 (예 : Return to Map 10초 타이머 후 지도 화면 자동 복귀 확인)

    @Column(length = 50)
    private String jiraTicketKey;               // 티켓 번호 (예 : INDIAPIOUP-4922)

    @Column(nullable = false)
    private String fileName;                    // 업로드 된 파일 명

    @Column(nullable = false)
    private String storedFilePath;              // 서버 디스크에 저장된 실제 파일 경로

    @Column(nullable = false)
    private Long fileSize;                      // 업로드 된 파일 크기 (bytes)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessStatus status;               // 분석 상태 (PENDING, PROCESSING, COMPLETED, FAILED)

    @Builder
    public LogFileUpload(String title, String jiraTicketKey, String fileName, String storedFilePath, Long fileSize, ProcessStatus status) {
        this.title = title;
        this.jiraTicketKey = jiraTicketKey;
        this.fileName = fileName;
        this.storedFilePath = storedFilePath;
        this.fileSize = fileSize;
        this.status = status != null ? status : ProcessStatus.PENDING;
    }

    // 비즈니스 메서드 : 분석 상태 변경
    public void updateStatus(ProcessStatus status) {
        this.status = status;
    }

    // 비즈니스 메서드 : 제목 수정
    public void updateTitle(String title) {
        this.title = title;
    }
}
