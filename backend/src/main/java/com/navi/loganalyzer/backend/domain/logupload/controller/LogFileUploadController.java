package com.navi.loganalyzer.backend.domain.logupload.controller;

import com.navi.loganalyzer.backend.domain.logupload.dto.LogFileUploadRequestDto;
import com.navi.loganalyzer.backend.domain.logupload.service.LogFileUploadService;
import com.navi.loganalyzer.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogFileUploadController {

    private final LogFileUploadService logFileUploadService;

    /**
     * [APi] 로그 파일 Upload
     * POST http://localhost:8081/api/v1/logs/upload
     */
    @PostMapping(value="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> uploadLogFile(
            @RequestPart("file")MultipartFile file,
            @Valid @RequestPart("request")LogFileUploadRequestDto requestDto) {

        Long Id = logFileUploadService.uploadLogFile(file, requestDto);
        return ResponseEntity.ok(ApiResponse.ok("로그 파일 저장이 완료되었습니다.", Id));
    }
}
