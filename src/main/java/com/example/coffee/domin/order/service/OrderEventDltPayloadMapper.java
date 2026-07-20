package com.example.coffee.domin.order.service;

import com.example.coffee.common.exception.BusinessException;
import com.example.coffee.common.exception.ErrorCode;
import com.example.coffee.domin.order.dto.OrderEventDltPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DLT 주문 이벤트 payload를 JSON 문자열로 직렬화하는 컴포넌트다.
 */
@Component
@RequiredArgsConstructor
public class OrderEventDltPayloadMapper {

    private final ObjectMapper objectMapper;

    // DLT payload DTO를 Kafka 전송용 JSON으로 변환한다.
    public String toJson(OrderEventDltPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
