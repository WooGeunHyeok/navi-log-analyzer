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
    private String title;

    @Column(name = "JIRA_TICKET_KEY", length = 50)
    private String jiraTicketKey;

    @Column(name = "ORIGINAL_FILE_NAME" ,nullable = false)
    private String originalFileName;

    @Column(name = "STORED_FILE_PATH", nullable = false)
    private String storedFilePath;

    @Column(name = "FILE_SIZE", nullable = false)
    private Long fileSize;

    @Column(name = "STATUS", nullable = false)
    private ProcessStatus status;
}
