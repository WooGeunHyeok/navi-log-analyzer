package com.navi.loganalyzer.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /**
     * HTTP 상태 코드와 함께 로그 분석 툴 전용 에러 코드
     */

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "존재하지 않는 리소스입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "입력 타입이 올바르지 않습니다."),

    // Log File & Parsing (내비게이션 로그 분석 전용)
    // TODO: 에러 로그 정리 필요
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "L001", "업로드된 로그 파일이 비어 있습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "L002", "지원하지 않는 로그 파일 형식입니다."),
    LOG_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "L003", "분석할 로그 파일을 찾을 수 없습니다."),
    LOG_PARSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "L004", "로그 파일 파싱 중 오류가 발생했습니다."),
    CORRUPTED_LOG_DATA(HttpStatus.UNPROCESSABLE_ENTITY, "L005", "손상되거나 읽을 수 없는 로그 데이터입니다."),
    INVALID_GPS_DATA(HttpStatus.BAD_REQUEST, "L006", "유효하지 않은 GPS 좌표/NMEA 데이터입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
