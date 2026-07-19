package com.example.coffee.domin.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.coffee.domin.menu.service.PopularMenuCacheService;
import com.example.coffee.domin.order.entity.OrderOutbox;
import com.example.coffee.domin.order.entity.OutboxStatus;
import com.example.coffee.domin.order.repository.OrderOutboxRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderEventRelayServiceTest {

    @InjectMocks
    private OrderEventRelayService orderEventRelayService;

    @Mock
    private OrderOutboxRepository orderOutboxRepository;

    @Mock
    private OrderEventSender orderEventSender;

    @Mock
    private PopularMenuCacheService popularMenuCacheService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderEventRelayService, "maxRetryCount", 3);
    }

    @Test
    @DisplayName("주문 직후 relay에 성공하면 outbox 상태가 SENT로 변경된다")
    void relayMarksOutboxSentWhenSendSucceeds() {
        OrderOutbox orderOutbox = createOutbox();
        given(orderOutboxRepository.findById(1L)).willReturn(Optional.of(orderOutbox));

        orderEventRelayService.relay(new OrderCreatedEvent(1L));

        assertThat(orderOutbox.getStatus()).isEqualTo(OutboxStatus.SENT);
        verify(popularMenuCacheService).recordOrder(orderOutbox.getOrder());
        verify(orderEventSender).send(orderOutbox.getPayload());
    }

    @Test
    @DisplayName("주문 직후 relay에 실패하면 retryCount가 증가하고 상태는 PENDING을 유지한다")
    void relayKeepsPendingWhenSendFailsBeforeMaxRetry() {
        OrderOutbox orderOutbox = createOutbox();
        given(orderOutboxRepository.findById(1L)).willReturn(Optional.of(orderOutbox));
        org.mockito.Mockito.doThrow(new RuntimeException("send failed"))
                .when(orderEventSender).send(orderOutbox.getPayload());

        orderEventRelayService.relay(new OrderCreatedEvent(1L));

        assertThat(orderOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(orderOutbox.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("재시도 대상 PENDING 이벤트를 다시 전송한다")
    void retryPendingEventsRetriesPendingOutbox() {
        OrderOutbox orderOutbox = createOutbox();
        given(orderOutboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(orderOutbox));

        orderEventRelayService.retryPendingEvents();

        assertThat(orderOutbox.getStatus()).isEqualTo(OutboxStatus.SENT);
        verify(orderEventSender).send(orderOutbox.getPayload());
    }

    @Test
    @DisplayName("재시도 최대 횟수를 초과하면 FAILED 상태로 전환된다")
    void retryPendingEventsMarksFailedWhenMaxRetryExceeded() {
        OrderOutbox orderOutbox = createOutbox();
        orderOutbox.markRetryFailure(3);
        orderOutbox.markRetryFailure(3);

        given(orderOutboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(orderOutbox));
        org.mockito.Mockito.doThrow(new RuntimeException("send failed"))
                .when(orderEventSender).send(orderOutbox.getPayload());

        orderEventRelayService.retryPendingEvents();

        assertThat(orderOutbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(orderOutbox.getRetryCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("이미 재시도 한도를 넘긴 이벤트는 다시 전송하지 않는다")
    void retryPendingEventsSkipsNonRetryableOutbox() {
        OrderOutbox orderOutbox = createOutbox();
        orderOutbox.markRetryFailure(1);

        given(orderOutboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(orderOutbox));

        orderEventRelayService.retryPendingEvents();

        verify(orderEventSender, never()).send(anyString());
    }

    private OrderOutbox createOutbox() {
        return OrderOutbox.pending(org.mockito.Mockito.mock(com.example.coffee.domin.order.entity.CoffeeOrder.class), "{\"orderId\":1}");
    }
}
