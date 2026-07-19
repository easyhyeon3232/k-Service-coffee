package com.example.coffee.domin.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.coffee.common.lock.RedisDistributedLockManager;
import com.example.coffee.domin.order.dto.OrderCreateResponse;
import java.time.Duration;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @InjectMocks
    private OrderFacade orderFacade;

    @Mock
    private RedisDistributedLockManager redisDistributedLockManager;

    @Mock
    private OrderService orderService;

    @Test
    @DisplayName("주문 결제 시 회원 단위 분산락을 적용한 뒤 서비스에 위임한다")
    void orderDelegatesToServiceWithDistributedLock() {
        OrderCreateResponse response = new OrderCreateResponse(100L, 1L, 10L, 3000L, 2000L);

        given(redisDistributedLockManager.execute(
                eq("lock:order:member:1"),
                eq(Duration.ofSeconds(3)),
                eq(Duration.ofSeconds(5)),
                any()
        )).willAnswer(invocation -> {
            Supplier<OrderCreateResponse> action = invocation.getArgument(3);
            return action.get();
        });
        given(orderService.order(1L, 10L)).willReturn(response);

        OrderCreateResponse result = orderFacade.order(1L, 10L);

        assertThat(result).isEqualTo(response);
        verify(orderService).order(1L, 10L);
    }
}
