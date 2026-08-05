package com.navi.loganalyzer.backend.domain.log.service;

import com.navi.loganalyzer.backend.domain.log.dto.LogFileUploadRequestDto;
import com.navi.loganalyzer.backend.domain.log.entity.LogFileUpload;
import com.navi.loganalyzer.backend.domain.log.entity.ProcessStatus;
import com.navi.loganalyzer.backend.domain.log.repository.LogFileUploadRepository;
import com.navi.loganalyzer.backend.global.exception.CustomException;
import com.navi.loganalyzer.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogFileUploadService {

    /**
     * 로그 파일 업로드 및 메타데이터 DB 저장
     */
    private final LogFileUploadRepository logFileRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public Long uploadLogFile(MultipartFile file, LogFileUploadRequestDto requestDto) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_EMPTY);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        try {
            // 저장 디렉토리 생성 (없으면 자동 생성)
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // UUID 생성 (파일명 중복 방지)
            String storeFileName = UUID.randomUUID() + "_" + originalFilename;
            Path targetPath = uploadPath.resolve(storeFileName);

            // 디스크에 파일 저장
            file.transferTo(targetPath.toFile());
            log.info("로그 파일 저장 완료 : {}", targetPath);

            // DB에 메타데이터 저장
            LogFileUpload logFile = LogFileUpload.builder()
                    .title(requestDto.getTitle())
                    .jiraTicketKey(requestDto.getJiraTicketKey())
                    .fileName(originalFilename)
                    .storedFilePath(targetPath.toString())
                    .fileSize(file.getSize())
                    .status(ProcessStatus.PENDING)
                    .build();

            LogFileUpload saveLogFileUpload = logFileRepository.save(logFile);

            return saveLogFileUpload.getId();

        } catch (IOException e) {
            log.error("파일 저장 중 IOException 발생", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }
    }

    /**
     * 단건 로그 파일 조회
     */
    public LogFileUpload getLogFile(Long LogFileId) {
        return logFileRepository.findById(LogFileId)
                .orElseThrow(() -> new CustomException(ErrorCode.LOG_FILE_NOT_FOUND));
    }

    /**
     * 전체 로그 파일 목록 조회
     */
    public List<LogFileUpload>  getAllLogFiles() {
        return logFileRepository.findAll();
    }

    /**
     * JIRA 티켓 번호로 로그 파일 목록 조회
     */
    public List<LogFileUpload> getLogFileByJiraTicket(String jiraTicketKey) {
        return logFileRepository.findByJiraTicketKey(jiraTicketKey);
    }
}
