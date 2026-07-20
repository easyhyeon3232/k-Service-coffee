package com.example.coffee.domin.order.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderEventPayloadMapperTest {

    private final OrderEventPayloadMapper orderEventPayloadMapper =
            new OrderEventPayloadMapper(new ObjectMapper().findAndRegisterModules());

    @Test
    @DisplayName("잘못된 주문 이벤트 payload는 복구 불가능 예외를 던진다")
    void fromJsonThrowsUnrecoverableExceptionWhenPayloadIsInvalid() {
        assertThrows(
                OrderEventUnrecoverableException.class,
                () -> orderEventPayloadMapper.fromJson("invalid-json")
        );
    }
}
