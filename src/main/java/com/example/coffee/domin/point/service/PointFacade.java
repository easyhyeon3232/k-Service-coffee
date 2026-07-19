package com.example.coffee.domin.point.service;

import com.example.coffee.common.lock.RedisDistributedLockManager;
import com.example.coffee.domin.point.dto.PointChargeResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 포인트 충전 요청에 회원 단위 분산락을 적용하는 파사드다.
 */
@Service
@RequiredArgsConstructor
public class PointFacade {

    private static final Duration WAIT_TIME = Duration.ofSeconds(3);
    private static final Duration LEASE_TIME = Duration.ofSeconds(5);

    private final RedisDistributedLockManager redisDistributedLockManager;
    private final PointService pointService;

    // 같은 회원의 포인트 충전 요청을 분산락으로 직렬화한다.
    public PointChargeResponse charge(Long memberId, long amount) {
        return redisDistributedLockManager.execute(
                createLockKey(memberId),
                WAIT_TIME,
                LEASE_TIME,
                () -> pointService.charge(memberId, amount)
        );
    }

    private String createLockKey(Long memberId) {
        return "lock:point:member:" + memberId;
    }
}
