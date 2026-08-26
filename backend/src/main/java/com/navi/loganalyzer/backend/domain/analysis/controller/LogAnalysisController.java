package com.navi.loganalyzer.backend.domain.analysis.controller;

import com.navi.loganalyzer.backend.domain.analysis.dto.LogAnalysisResponseDto;
import com.navi.loganalyzer.backend.domain.analysis.service.LogAnalysisService;
import com.navi.loganalyzer.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logs/analysis")
@RequiredArgsConstructor
public class LogAnalysisController {

    private final LogAnalysisService logAnalysisService;

    /**
     * [API] 로그 파일 분석 (사전 DB 매칭 + 결과 저장)
     * GET http://localhost:8081/api/v1/logs/analysis/flow/{logFileId}
     */
    @GetMapping("/flow/{logFileId}")
    public ResponseEntity<ApiResponse<List<LogAnalysisResponseDto>>> getFileFlow(
            @PathVariable("logFileId") Long logFileId) {

        List<LogAnalysisResponseDto> treeResponse = logAnalysisService.getLogFlowTree(logFileId);
        return ResponseEntity.ok(ApiResponse.ok("로그 파일 분석 FLOW 조회가 완료되었습니다.", treeResponse));
    }

    /**
     * [API] 이미 분석/저장된 결과 조회 (재매칭 없이 TBL_LOG_ANALYSIS에서 바로 조회)
     * GET http://localhost:8081/api/v1/logs/analysis/result/{logFileId}
     */
    @GetMapping("/result/{logFileId}")
    public ResponseEntity<ApiResponse<List<LogAnalysisResponseDto>>> getSavedFileFlow(
            @PathVariable("logFileId") Long logFileId) {

        List<LogAnalysisResponseDto> treeResponse = logAnalysisService.getSavedLogFlowTree(logFileId);
        return ResponseEntity.ok(ApiResponse.ok("저장된 로그 분석 결과 조회가 완료되었습니다.", treeResponse));
    }
}
