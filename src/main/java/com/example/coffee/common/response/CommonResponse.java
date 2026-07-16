package com.example.coffee.common.response;

/**
 * API 성공/실패 응답을 공통 형식으로 감싸는 응답 DTO다.
 *
 * @param success 요청 성공 여부
 * @param code 응답 코드
 * @param message 응답 메시지
 * @param data 실제 응답 데이터
 * @param <T> 응답 데이터 타입
 */
public record CommonResponse<T>(boolean success, String code, String message, T data) {

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(true, "SUCCESS", "요청이 성공했습니다.", data);
    }

    public static <T> CommonResponse<T> fail(String code, String message) {
        return new CommonResponse<>(false, code, message, null);
    }
}
