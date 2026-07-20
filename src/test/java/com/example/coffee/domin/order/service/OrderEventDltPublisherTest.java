package com.example.coffee.domin.order.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.coffee.domin.order.dto.OrderEventDltPayload;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderEventDltPublisherTest {

    @InjectMocks
    private OrderEventDltPublisher orderEventDltPublisher;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private OrderEventDltPayloadMapper orderEventDltPayloadMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderEventDltPublisher, "dltTopic", "coffee.order.created.dlt");
        ReflectionTestUtils.setField(orderEventDltPublisher, "retryMaxAttempts", 3);
    }

    @Test
    @DisplayName("재시도 가능한 예외는 최대 재시도 횟수와 함께 DLT로 보낸다")
    void publishSendsRetriableFailureToDlt() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("coffee.order.created", 1, 10L, "1", "{\"orderId\":1}");
        ArgumentCaptor<OrderEventDltPayload> payloadCaptor = ArgumentCaptor.forClass(OrderEventDltPayload.class);
        given(orderEventDltPayloadMapper.toJson(payloadCaptor.capture())).willReturn("{\"dlt\":true}");
        given(kafkaTemplate.send(eq("coffee.order.created.dlt"), eq("1"), eq("{\"dlt\":true}")))
                .willReturn(CompletableFuture.completedFuture(null));

        orderEventDltPublisher.publish(record, new RuntimeException("temporary failure"));

        verify(kafkaTemplate).send("coffee.order.created.dlt", "1", "{\"dlt\":true}");
        org.junit.jupiter.api.Assertions.assertEquals(3, payloadCaptor.getValue().retryCount());
        org.junit.jupiter.api.Assertions.assertEquals("temporary failure".contains("temporary"), true);
    }

    @Test
    @DisplayName("복구 불가능한 예외는 즉시 DLT로 보낸다")
    void publishSendsUnrecoverableFailureToDltImmediately() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("coffee.order.created", 1, 10L, "1", "{\"orderId\":1}");
        ArgumentCaptor<OrderEventDltPayload> payloadCaptor = ArgumentCaptor.forClass(OrderEventDltPayload.class);
        given(orderEventDltPayloadMapper.toJson(payloadCaptor.capture())).willReturn("{\"dlt\":true}");
        given(kafkaTemplate.send(eq("coffee.order.created.dlt"), eq("1"), eq("{\"dlt\":true}")))
                .willReturn(CompletableFuture.completedFuture(null));

        orderEventDltPublisher.publish(
                record,
                new OrderEventUnrecoverableException("invalid payload", new RuntimeException())
        );

        verify(kafkaTemplate).send("coffee.order.created.dlt", "1", "{\"dlt\":true}");
        org.junit.jupiter.api.Assertions.assertEquals(0, payloadCaptor.getValue().retryCount());
    }
}
