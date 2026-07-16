package com.example.coffee.domin.order.dto;

import com.example.coffee.domin.order.entity.CoffeeOrder;

public record OrderCreateResponse(
        Long orderId,
        Long memberId,
        Long menuId,
        long orderPrice,
        long remainingPoint
) {

    // 주문 결과와 남은 포인트를 응답 DTO로 변환한다.
    public static OrderCreateResponse of(CoffeeOrder coffeeOrder, long remainingPoint) {
        return new OrderCreateResponse(
                coffeeOrder.getId(),
                coffeeOrder.getMember().getId(),
                coffeeOrder.getMenu().getId(),
                coffeeOrder.getOrderPrice(),
                remainingPoint
        );
    }
}
