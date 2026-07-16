package com.example.coffee.common.exception;

/**
 * 전역 예외 응답에 사용할 공통 응답 DTO다.
 *
 * @param code 에러코드
 * @param message 에러 메시지
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}
