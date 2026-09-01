package com.navi.loganalyzer.backend.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class LogFlowResultDto {

    /**
     * 소스코드 흐름도 API 최종 응답 - 흐름도 트리와 사전 조건 판별용 시스템 로그를 함께 담는다.
     */

    private List<LogAnalysisResponseDto> tree;
    private List<SystemLogResponseDto> systemLogs;
}
