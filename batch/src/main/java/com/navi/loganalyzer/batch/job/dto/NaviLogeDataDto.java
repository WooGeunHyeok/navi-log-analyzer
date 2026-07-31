package com.navi.loganalyzer.batch.job.dto;

import com.navi.loganalyzer.batch.entity.NaviLogeData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaviLogeDataDto {

    private String fileName;                    // 파일명
    private String filePath;                    // 전체 파일 경로
    private String functionName;                // 함수명
    private Integer lineNumber;                 // 소스코드 줄 번호
    private String logType;                     // 로그 타입 (ZLOG, ZLOGA, LOGE)
    private String rawMessage;                  // 원본 로그 메시지
    private String patternMessage;              // DB 매칭용 정규식 패턴 (.* 변환 메시지)

    /**
     * DTO -> Entity 변환 메서드
     * Processor 단계에서 ItemWriter로 넘겨줄 Entity 객체를 만들 때 사용
     */
    public NaviLogeData toEntity() {
        return NaviLogeData.builder()
                .fileName(this.fileName)
                .filePath(this.filePath)
                .functionName(this.functionName)
                .lineNumber(this.lineNumber)
                .logType(this.logType)
                .rawMessage(this.rawMessage)
                .patternMessage(this.patternMessage)
                .build();
    }
}
