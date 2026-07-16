package com.example.coffee.common.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 사용하는 공통 예외다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
