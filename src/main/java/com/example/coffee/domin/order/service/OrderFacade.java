package com.example.coffee.domin.order.service;

import com.example.coffee.common.lock.RedisDistributedLockManager;
import com.example.coffee.domin.order.dto.OrderCreateResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 주문 결제 요청에 회원 단위 분산락을 적용하는 파사드다.
 */
@Service
@RequiredArgsConstructor
public class OrderFacade {

    private static final Duration WAIT_TIME = Duration.ofSeconds(3);
    private static final Duration LEASE_TIME = Duration.ofSeconds(5);

    private final RedisDistributedLockManager redisDistributedLockManager;
    private final OrderService orderService;

    // 같은 회원의 주문 결제 요청을 분산락으로 직렬화한다.
    public OrderCreateResponse order(Long memberId, Long menuId) {
        return redisDistributedLockManager.execute(
                createLockKey(memberId),
                WAIT_TIME,
                LEASE_TIME,
                () -> orderService.order(memberId, menuId)
        );
    }

    private String createLockKey(Long memberId) {
        return "lock:order:member:" + memberId;
    }
}
