package com.example.coffee.domin.point.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.coffee.domin.point.entity.PointHistoryType;
import com.example.coffee.domin.point.repository.PointHistoryRepository;
import com.example.coffee.domin.point.repository.PointWalletRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_INTEGRATION_TEST", matches = "true")
class PointDistributedLockIntegrationTest {

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PointWalletRepository pointWalletRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from point_history");
        jdbcTemplate.update("delete from coffee_order");
        jdbcTemplate.update("delete from order_outbox");
        jdbcTemplate.update("delete from point_wallet");
        jdbcTemplate.update("delete from coffee_menu");
        jdbcTemplate.update("delete from member");

        jdbcTemplate.update("insert into member (id, created_at, updated_at) values (1, now(), now())");
        clearLock("lock:point:member:1");
    }

    @Test
    @DisplayName("같은 회원의 포인트 충전 요청이 동시에 들어와도 최종 잔액과 이력이 정합성을 유지한다")
    void chargeMaintainsConsistencyUnderConcurrentRequests() throws InterruptedException {
        int threadCount = 10;
        AtomicInteger successCount = new AtomicInteger();
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        for (int index = 0; index < threadCount; index++) {
            executorService.submit(() -> {
                readyLatch.countDown();
                await(startLatch);
                try {
                    pointFacade.charge(1L, 1000L);
                    successCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        long balance = pointWalletRepository.findByMemberId(1L).orElseThrow().getBalance();
        long chargeHistoryCount = pointHistoryRepository.countByMemberIdAndType(1L, PointHistoryType.CHARGE);

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(balance).isEqualTo(10_000L);
        assertThat(chargeHistoryCount).isEqualTo(threadCount);
    }

    private void clearLock(String key) {
        stringRedisTemplate.delete(key);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
