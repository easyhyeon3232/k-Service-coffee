package com.example.coffee.domin.order.service;

import com.example.coffee.domin.order.entity.OrderOutbox;
import com.example.coffee.domin.order.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
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

    // 주문 커밋 이후 outbox 이벤트를 외부 플랫폼으로 전송한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void relay(OrderCreatedEvent event) {
        OrderOutbox orderOutbox = orderOutboxRepository.findById(event.outboxId()).orElseThrow();

        try {
            orderEventSender.send(orderOutbox.getPayload());
            orderOutbox.markSent();
        } catch (RuntimeException exception) {
            orderOutbox.markFailed();
        }
    }
}
