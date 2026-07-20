package com.example.coffee.domin.order.dto;

import java.time.LocalDateTime;

public record OrderEventPayload(
        Long orderId,
        Long memberId,
        Long menuId,
        long orderPrice,
        LocalDateTime orderedAt
) {
}
