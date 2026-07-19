package com.example.coffee.domin.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PENDING 상태 outbox 이벤트를 주기적으로 재시도하는 스케줄러다.
 */
@Component
@RequiredArgsConstructor
public class OutboxRetryScheduler {

    private final OrderEventRelayService orderEventRelayService;

    // 주기적으로 미전송 outbox 이벤트를 재시도한다.
    @Scheduled(fixedDelayString = "${outbox.retry.delay-ms:2000}")
    public void retryPendingEvents() {
        orderEventRelayService.retryPendingEvents();
    }
}
