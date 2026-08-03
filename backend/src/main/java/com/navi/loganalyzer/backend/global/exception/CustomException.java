package com.navi.loganalyzer.backend.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    /**
     * ErrorCode를 품고 던져지는 비즈니스 전용 예외 클래스입니다.
     */

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
