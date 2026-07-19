package com.example.coffee.domin.order.service;

import com.example.coffee.domin.order.entity.OrderOutbox;
import com.example.coffee.domin.order.entity.OutboxStatus;
import com.example.coffee.domin.order.repository.OrderOutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 커밋 이후 outbox 이벤트를 외부 전송기로 전달하는 서비스다.
 */
@Service
@RequiredArgsConstructor
public class OrderEventRelayService {

    private final OrderOutboxRepository orderOutboxRepository;
    private final OrderEventSender orderEventSender;

    @Value("${outbox.retry.max-count:3}")
    private int maxRetryCount;

    // 주문 커밋 이후 outbox 이벤트를 바로 외부 플랫폼으로 전송한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void relay(OrderCreatedEvent event) {
        OrderOutbox orderOutbox = orderOutboxRepository.findById(event.outboxId()).orElseThrow();
        send(orderOutbox);
    }

    // 재시도 대상 PENDING 이벤트를 순서대로 다시 전송한다.
    @Transactional
    public void retryPendingEvents() {
        List<OrderOutbox> retryTargets = orderOutboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        retryTargets.stream()
                .filter(orderOutbox -> orderOutbox.canRetry(maxRetryCount))
                .forEach(this::send);
    }

    private void send(OrderOutbox orderOutbox) {
        try {
            orderEventSender.send(orderOutbox.getPayload());
            orderOutbox.markSent();
        } catch (RuntimeException exception) {
            orderOutbox.markRetryFailure(maxRetryCount);
        }
    }
}
