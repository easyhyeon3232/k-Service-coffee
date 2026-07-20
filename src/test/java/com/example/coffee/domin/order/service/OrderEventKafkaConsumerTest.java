package com.example.coffee.domin.order.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class OrderEventKafkaConsumerTest {

    @InjectMocks
    private OrderEventKafkaConsumer orderEventKafkaConsumer;

    @Mock
    private OrderEventConsumerService orderEventConsumerService;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    @DisplayName("Kafka listener는 처리 완료 후 수동 ack를 호출한다")
    void consumeDelegatesToServiceAndAcknowledges() {
        orderEventKafkaConsumer.consume(
                "{\"orderId\":1}",
                "coffee.order.created",
                "1",
                0,
                15L,
                acknowledgment
        );

        verify(orderEventConsumerService).consume("coffee.order.created", 0, 15L, 1L, "{\"orderId\":1}");
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("listener 처리 중 예외가 발생하면 ack 하지 않는다")
    void consumeDoesNotAcknowledgeWhenServiceFails() {
        willThrow(new RuntimeException("temporary failure"))
                .given(orderEventConsumerService)
                .consume("coffee.order.created", 0, 15L, 1L, "{\"orderId\":1}");

        assertThrows(
                RuntimeException.class,
                () -> orderEventKafkaConsumer.consume(
                        "{\"orderId\":1}",
                        "coffee.order.created",
                        "1",
                        0,
                        15L,
                        acknowledgment
                )
        );

        verify(acknowledgment, never()).acknowledge();
    }
}
