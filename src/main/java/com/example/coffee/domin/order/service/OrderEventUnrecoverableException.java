package com.example.coffee.domin.order.service;

/**
 * 재시도해도 복구할 수 없는 주문 이벤트 소비 실패를 나타내는 예외다.
 */
public class OrderEventUnrecoverableException extends RuntimeException {

    public OrderEventUnrecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
