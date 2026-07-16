package com.example.coffee.domin.order.controller;

import com.example.coffee.common.response.CommonResponse;
import com.example.coffee.domin.order.dto.OrderCreateRequest;
import com.example.coffee.domin.order.dto.OrderCreateResponse;
import com.example.coffee.domin.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 커피 주문과 포인트 결제 요청을 처리하는 API 컨트롤러다.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 회원이 메뉴를 주문하고 포인트로 결제한다.
    @PostMapping
    public ResponseEntity<CommonResponse<OrderCreateResponse>> order(
            @Valid @RequestBody OrderCreateRequest request
    ) {
        OrderCreateResponse response = orderService.order(request.memberId(), request.menuId());
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
