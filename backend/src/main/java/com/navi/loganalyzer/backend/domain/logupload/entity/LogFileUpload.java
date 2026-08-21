package com.navi.loganalyzer.backend.domain.logupload.entity;

import com.navi.loganalyzer.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TBL_LOGS_FILES")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LogFileUpload extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "TITLE", nullable = false, length = 100)
    private String title;                       // 로그 제목 (예 : Return to Map 10초 타이머 후 지도 화면 자동 복귀 확인)

    @Column(name = "JIRA_TICKET_KEY", length = 50)
    private String jiraTicketKey;               // 티켓 번호 (예 : INDIAPIOUP-4922)

    @Column(name = "ORIGINAL_FILE_NAME" ,nullable = false)
    private String originalFileName;                    // 업로드 된 파일 명

    @Column(name = "STORED_FILE_PATH", nullable = false)
    private String storedFilePath;              // 서버 디스크에 저장된 실제 파일 경로

    @Column(name = "FILE_SIZE", nullable = false)
    private Long fileSize;                      // 업로드 된 파일 크기 (bytes)

    @Column(name = "STATUS", nullable = false)
    private ProcessStatus status;
}
