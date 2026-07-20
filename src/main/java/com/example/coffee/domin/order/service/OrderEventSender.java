package com.example.coffee.domin.order.service;

/**
 * 주문 Outbox 이벤트를 외부 메시지 브로커로 전달하는 전송 인터페이스다.
 */
public interface OrderEventSender {

    // 주문 ID를 메시지 키로 사용해 이벤트를 외부 브로커로 발행한다.
    void send(Long orderId, String payload);
}
