package com.navi.loganalyzer.backend.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    /**
     * 성공 시 데이터 (data), 상태 메시지 (message), 상태코드 (status)를 감싸서 내려주는 제너릭 Wrapper 클래스
     */

    private final boolean success;
    private final String message;
    private final T data;

    // 데이터 포함 성공 응답
    public static <T> ApiResponse<T> ok(T data) {return new ApiResponse<>(true, null, data);}

    // 메시지 및 데이터 포함 성공 응답
    public static <T> ApiResponse<T> ok(String message, T data) {return new ApiResponse<>(true, message, data);}

    // 메시지만 포함하는 성공 응답
    public static ApiResponse<Void> ok(String message) {return new ApiResponse<>(true, message, null);}

    // 실패 응답 (간단한 메시지 전달용)
    public static ApiResponse<Void> fail(String message) {return new ApiResponse<>(false, message, null);}
}
