package com.navi.loganalyzer.backend.domain.parsing.controller;

import com.navi.loganalyzer.backend.domain.parsing.dto.ParsingLogFlowResponseDto;
import com.navi.loganalyzer.backend.domain.parsing.service.ParsingLogService;
import com.navi.loganalyzer.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parsing")
@RequiredArgsConstructor
public class ParsingController {

    private final ParsingLogService parsingLogService;

    /**
     * [API] 로그 파일 파싱 실행
     * POST http://localhost:8081/api/v1/parsing/{logFileId}
     */
    @PostMapping("/{logFileId}")
    public ResponseEntity<ApiResponse<Long>> parseLogFile(
            @PathVariable("logFileId") Long logFileId) {

        Long ParsingFileId = parsingLogService.parsingAndSaveLogFile(logFileId);

        return ResponseEntity.ok(ApiResponse.ok("로그 파일 파싱이 완료되었습니다.", ParsingFileId));
    }

    /**
     * [API] 파싱 결과 및 소스코드 매핑 흐름 조회
     * GET http://localhost:8081/api/v1/parsing/{logFileId}/flow
     */
    @GetMapping("/{logFileId}/flow")
    public ResponseEntity<ApiResponse<List<ParsingLogFlowResponseDto>>> getParsingLogFlow(
            @PathVariable("logFileId") Long logFileId) {
        List<ParsingLogFlowResponseDto> responseDto = parsingLogService.getParsingLogFlow(logFileId);

        return ResponseEntity.ok(ApiResponse.ok("로그 플로우 조회가 완료되었습니다.", responseDto));
    }
}
