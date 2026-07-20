package com.example.coffee.domin.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.coffee.domin.order.dto.OrderEventPayload;
import com.example.coffee.domin.order.repository.OrderEventConsumeLogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerServiceTest {

    @InjectMocks
    private OrderEventConsumerService orderEventConsumerService;

    @Mock
    private OrderEventConsumeLogRepository orderEventConsumeLogRepository;

    @Mock
    private OrderEventPayloadMapper orderEventPayloadMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderEventConsumerService, "consumerGroupId", "coffee-order-consumer");
    }

    @Test
    @DisplayName("처음 읽은 주문 이벤트는 소비 로그로 저장한다")
    void consumeSavesConsumeLogWhenOrderIsNew() {
        given(orderEventConsumeLogRepository.existsByOrderId(1L)).willReturn(false);
        given(orderEventPayloadMapper.fromJson("{\"orderId\":1}"))
                .willReturn(new OrderEventPayload(1L, 10L, 20L, 3000L, LocalDateTime.now()));

        orderEventConsumerService.consume("coffee.order.created", 0, 15L, 1L, "{\"orderId\":1}");

        verify(orderEventPayloadMapper).fromJson("{\"orderId\":1}");
        verify(orderEventConsumeLogRepository).save(any());
    }

    @Test
    @DisplayName("이미 처리한 주문 이벤트는 중복 소비를 건너뛴다")
    void consumeSkipsWhenOrderWasAlreadyConsumed() {
        given(orderEventConsumeLogRepository.existsByOrderId(1L)).willReturn(true);

        orderEventConsumerService.consume("coffee.order.created", 0, 15L, 1L, "{\"orderId\":1}");

        verify(orderEventPayloadMapper, never()).fromJson(any());
        verify(orderEventConsumeLogRepository, never()).save(any());
    }
}
