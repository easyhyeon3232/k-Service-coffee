package com.example.coffee.domin.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.coffee.common.lock.RedisDistributedLockManager;
import com.example.coffee.domin.point.dto.PointChargeResponse;
import java.time.Duration;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointFacadeTest {

    @InjectMocks
    private PointFacade pointFacade;

    @Mock
    private RedisDistributedLockManager redisDistributedLockManager;

    @Mock
    private PointService pointService;

    @Test
    @DisplayName("포인트 충전 시 회원 단위 분산락을 적용한 뒤 서비스에 위임한다")
    void chargeDelegatesToServiceWithDistributedLock() {
        PointChargeResponse response = new PointChargeResponse(1L, 1000L, 3000L);

        given(redisDistributedLockManager.execute(
                eq("lock:point:member:1"),
                eq(Duration.ofSeconds(3)),
                eq(Duration.ofSeconds(5)),
                any()
        )).willAnswer(invocation -> {
            Supplier<PointChargeResponse> action = invocation.getArgument(3);
            return action.get();
        });
        given(pointService.charge(1L, 1000L)).willReturn(response);

        PointChargeResponse result = pointFacade.charge(1L, 1000L);

        assertThat(result).isEqualTo(response);
        verify(pointService).charge(1L, 1000L);
    }
}
