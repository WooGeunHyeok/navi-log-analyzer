package com.navi.loganalyzer.batch.entity;

import com.navi.loganalyzer.batch.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NaviLogeData extends BaseEntity {

    private Long id;                    // LOGE 고유 ID (PK)
    private String fileName;            // 파일명
    private String filePath;            // 전체 파일 경로
    private String functionName;        // 함수명
    private Integer lineNumber;         // 소스코드 줄 번호
    private String logType;             // 로그 타입 (ZLOG, ZLOGA, LOGE)
    private String rawMessage;          // 원본 로그 메시지
    private String patternMessage;      // DB 매칭용 정규식 패턴 (.* 변환 메시지)
    private String calls;               // 하위 호출 함수/심볼 목록
}
