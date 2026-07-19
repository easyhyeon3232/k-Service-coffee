package com.example.coffee.domin.order.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxRetrySchedulerTest {

    @InjectMocks
    private OutboxRetryScheduler outboxRetryScheduler;

    @Mock
    private OrderEventRelayService orderEventRelayService;

    @Test
    @DisplayName("스케줄러가 실행되면 pending outbox 재시도를 위임한다")
    void retryPendingEventsDelegatesToRelayService() {
        outboxRetryScheduler.retryPendingEvents();

        verify(orderEventRelayService).retryPendingEvents();
    }
}
