package com.example.coffee.domin.order.service;

import com.example.coffee.common.exception.BusinessException;
import com.example.coffee.common.exception.ErrorCode;
import com.example.coffee.domin.order.dto.OrderEventPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 주문 이벤트 payload를 JSON 문자열과 DTO로 변환하는 컴포넌트다.
 */
@Component
@RequiredArgsConstructor
public class OrderEventPayloadMapper {

    private final ObjectMapper objectMapper;

    // 주문 이벤트 DTO를 Kafka payload JSON으로 직렬화한다.
    public String toJson(OrderEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // Kafka에서 받은 주문 이벤트 JSON을 DTO로 역직렬화한다.
    public OrderEventPayload fromJson(String payload) {
        try {
            return objectMapper.readValue(payload, OrderEventPayload.class);
        } catch (JsonProcessingException exception) {
            throw new OrderEventUnrecoverableException("주문 이벤트 payload 역직렬화에 실패했습니다.", exception);
        }
    }
}
