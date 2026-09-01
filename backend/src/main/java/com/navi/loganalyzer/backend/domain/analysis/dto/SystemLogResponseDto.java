package com.navi.loganalyzer.backend.domain.analysis.dto;

import com.navi.loganalyzer.backend.domain.parsing.entity.SystemLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SystemLogResponseDto {

    /**
     * 사전 조건 판별용 시스템 로그 Response Dto.
     * 트리 전용 필드(layer/matchType/fileName/functionName/children)는 없고,
     * 원본 태그(tag)를 그대로 노출해서 프론트가 자유롭게 매칭하도록 한다.
     */

    private Long id;
    private Long fileId;
    private String timestamp;
    private Long threadId;
    private String logLevel;
    private String tag;
    private String rawMessage;

    public static SystemLogResponseDto fromEntity(SystemLog entity) {
        if (entity == null) {
            return null;
        }

        return SystemLogResponseDto.builder()
                .id(entity.getId())
                .fileId(entity.getLogFile() != null ? entity.getLogFile().getId() : null)
                .timestamp(entity.getTimestamp())
                .threadId(entity.getThreadId())
                .logLevel(entity.getLogLevel())
                .tag(entity.getTag())
                .rawMessage(entity.getRawMessage())
                .build();
    }
}
