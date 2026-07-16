package com.example.coffee.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 공통으로 사용할 에러코드 상수다.
 */
@Getter
public enum ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "P001", "충전 금액은 0보다 커야 합니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "P002", "포인트가 부족합니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "메뉴를 찾을 수 없습니다."),
    MENU_NOT_ON_SALE(HttpStatus.BAD_REQUEST, "M002", "현재 판매 중인 메뉴가 아닙니다."),
    MENU_LIST_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M003", "메뉴 목록 조회에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

}
