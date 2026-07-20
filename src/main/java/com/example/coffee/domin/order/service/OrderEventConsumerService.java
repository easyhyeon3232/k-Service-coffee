package com.example.coffee.domin.order.service;

import com.example.coffee.domin.order.dto.OrderEventPayload;
import com.example.coffee.domin.order.entity.OrderEventConsumeLog;
import com.example.coffee.domin.order.repository.OrderEventConsumeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka에서 읽은 주문 이벤트를 실제로 처리하고 중복 소비를 방지하는 서비스다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumerService {

    private final OrderEventConsumeLogRepository orderEventConsumeLogRepository;
    private final OrderEventPayloadMapper orderEventPayloadMapper;

    @Value("${order-event.kafka.consumer.group-id:coffee-order-consumer}")
    private String consumerGroupId;

    // 주문 이벤트를 처리하고 같은 주문이 이미 처리된 경우에는 중복 소비를 건너뛴다.
    @Transactional
    public void consume(String topic, int partition, long offset, Long orderId, String payload) {
        if (orderEventConsumeLogRepository.existsByOrderId(orderId)) {
            log.info("duplicated order event skipped. orderId={}, partition={}, offset={}", orderId, partition, offset);
            return;
        }

        OrderEventPayload eventPayload = orderEventPayloadMapper.fromJson(payload);
        log.info(
                "order event consumed. orderId={}, memberId={}, menuId={}, orderPrice={}, partition={}, offset={}",
                eventPayload.orderId(),
                eventPayload.memberId(),
                eventPayload.menuId(),
                eventPayload.orderPrice(),
                partition,
                offset
        );

        orderEventConsumeLogRepository.save(
                OrderEventConsumeLog.create(orderId, consumerGroupId, topic, partition, offset, payload)
        );
    }
}
