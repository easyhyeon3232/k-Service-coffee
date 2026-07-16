package com.example.coffee.domin.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 데이터 수집 플랫폼 전송을 대체하는 mock 전송 컴포넌트다.
 */
@Slf4j
@Component
public class MockOrderEventSender implements OrderEventSender {

    @Override
    public void send(String payload) {
        log.info("mock order event sent. payload={}", payload);
    }
}
