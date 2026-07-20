package com.example.coffee.domin.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Kafka 토픽에서 주문 이벤트를 읽어 consumer 서비스로 전달하는 listener다.
 */
@Component
@RequiredArgsConstructor
public class OrderEventKafkaConsumer {

    private final OrderEventConsumerService orderEventConsumerService;

    // 메시지 처리와 소비 로그 저장이 끝난 뒤에만 Kafka offset을 수동 커밋한다.
    @KafkaListener(
            topics = "${order-event.kafka.topic}",
            groupId = "${order-event.kafka.consumer.group-id}",
            concurrency = "${order-event.kafka.consumer.concurrency:3}"
    )
    public void consume(
            String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String orderIdKey,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        orderEventConsumerService.consume(topic, partition, offset, Long.valueOf(orderIdKey), payload);
        acknowledgment.acknowledge();
    }
}
