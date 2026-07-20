package com.example.coffee.domin.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 주문 Outbox 이벤트를 Kafka 토픽으로 발행하는 컴포넌트다.
 */
@Component
@RequiredArgsConstructor
public class KafkaOrderEventSender implements OrderEventSender {

    // kafka로 편지 보내는 도구
    private final KafkaTemplate<String, String> kafkaTemplate;

    // kafka 안의 편지함 이름
    @Value("${order-event.kafka.topic}")
    private String topic;

    @Override
    // 주문 ID를 Kafka 메시지 키로 사용해 같은 주문 이벤트를 추적하기 쉽게 만든다.
    // orderId : 주문 번호 / payload : 실제 편지 내용(JSON 문자열)
    public void send(Long orderId, String payload) {
        // 토픽이라는 편지함에 orderId를 표지처럼 붙여서 payload내용을 보낸다.
        kafkaTemplate.send(topic, orderId.toString(), payload)
                .join();  // 진짜로 보내기 완료될 때까지 기다리겠다.
    }
}
