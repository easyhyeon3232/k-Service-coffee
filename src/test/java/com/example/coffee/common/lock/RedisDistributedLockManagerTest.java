package com.example.coffee.common.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.coffee.common.exception.BusinessException;
import com.example.coffee.common.exception.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisDistributedLockManagerTest {

    @InjectMocks
    private RedisDistributedLockManager redisDistributedLockManager;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("락 획득에 성공하면 작업을 수행하고 락을 해제한다")
    void executeRunsActionWhenLockIsAcquired() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(true);
        given(stringRedisTemplate.execute(any(), anyList(), anyString())).willReturn(1L);

        String result = redisDistributedLockManager.execute(
                "lock:point:member:1",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                () -> "success"
        );

        assertThat(result).isEqualTo("success");
        verify(stringRedisTemplate).execute(any(), anyList(), anyString());
    }

    @Test
    @DisplayName("락을 획득하지 못하면 분산락 예외를 반환한다")
    void executeThrowsBusinessExceptionWhenLockCannotBeAcquired() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(false);

        assertThatThrownBy(() -> redisDistributedLockManager.execute(
                "lock:order:member:1",
                Duration.ZERO,
                Duration.ofSeconds(2),
                () -> "fail"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DISTRIBUTED_LOCK_FAILED);

        verify(stringRedisTemplate, never()).execute(any(), anyList(), anyString());
    }
}
