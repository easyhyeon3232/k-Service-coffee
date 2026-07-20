package com.example.coffee.domin.order.service;

import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Kafka 전송 구현이 올바른 토픽과 메시지 키로 발행하는지 검증하는 테스트다.
 */
@ExtendWith(MockitoExtension.class)
class KafkaOrderEventSenderTest {

    @InjectMocks
    private KafkaOrderEventSender kafkaOrderEventSender;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kafkaOrderEventSender, "topic", "coffee.order.created");
    }

    @Test
    @DisplayName("주문 이벤트를 Kafka 토픽으로 발행한다")
    void sendPublishesMessageToKafka() {
        // Kafka 브로커 없이도 send 호출 자체가 올바른지만 검증한다.
        org.mockito.BDDMockito.given(kafkaTemplate.send("coffee.order.created", "1", "{\"orderId\":1}"))
                .willReturn(CompletableFuture.completedFuture(new SendResult<>(null, (RecordMetadata) null)));

        kafkaOrderEventSender.send(1L, "{\"orderId\":1}");

        verify(kafkaTemplate).send("coffee.order.created", "1", "{\"orderId\":1}");
    }
}
