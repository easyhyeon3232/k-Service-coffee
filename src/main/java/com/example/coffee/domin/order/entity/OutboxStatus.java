package com.example.coffee.domin.order.entity;

/**
 * 외부 전송용 아웃박스 이벤트 상태를 표현한다.
 */
public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
