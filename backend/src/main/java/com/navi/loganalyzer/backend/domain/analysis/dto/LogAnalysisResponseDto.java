package com.navi.loganalyzer.backend.domain.analysis.dto;

import com.navi.loganalyzer.backend.domain.analysis.entity.LogAnalysis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class LogAnalysisResponseDto {

    /**
     * 로그 분석 Response Dto
     */

    private Long id;
    private Long fileId;
    private Long step;
    private String timestamp;
    private Long threadId;
    private String logLevel;
    private String layer;

    // 원본 로그 메세지
    private String rawMessage;

    // 소스코드 사전 매칭 정보
    private String fileName;
    private String filePath;
    private String functionName;
    private Integer lineNumber;
    private String matchType;

    // 계층 깊이 및 자식 호출 목록
    private int depth;

    @Builder.Default
    private List<LogAnalysisResponseDto> children = new ArrayList<>();

    /**
     * LogAnalysis 엔티티와 계산된 depth를 받아서 DTO로 변환하는 static 팩토리 메서드
     */
    public static LogAnalysisResponseDto fromEntity(LogAnalysis entity, int depth) {
        if (entity == null) {
            return null;
        }

        return LogAnalysisResponseDto.builder()
                // ParsingLog.id 기준의 안정적인 식별자를 응답에 노출한다.
                // entity.getId()(LogAnalysis 자체 PK)는 분석 직후 응답을 만들 때는 아직
                // DB 저장 전이라 null이므로 사용하지 않는다.
                .id(entity.getParsingLogId())
                .fileId(entity.getFileId())
                .step(entity.getStep())
                .timestamp(entity.getTimestamp())
                .threadId(entity.getThreadId())
                .logLevel(entity.getLogLevel())
                .layer(entity.getLayer())
                .rawMessage(entity.getRawMessage())
                .fileName(entity.getFileName())
                .filePath(entity.getFilePath())
                .functionName(entity.getFunctionName())
                .lineNumber(entity.getLineNumber())
                .matchType(entity.getMatchType())
                .depth(depth)
                .children(new ArrayList<>()) // 자식 노드 리스트 초기화
                .build();
    }
}
