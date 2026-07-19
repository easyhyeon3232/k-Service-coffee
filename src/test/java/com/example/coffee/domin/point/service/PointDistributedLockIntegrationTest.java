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

    private static final String POINT_LOCK_KEY = "lock:point:member:1";
    private static final String DELETE_POINT_HISTORY = "delete from point_history";
    private static final String DELETE_COFFEE_ORDER = "delete from coffee_order";
    private static final String DELETE_ORDER_OUTBOX = "delete from order_outbox";
    private static final String DELETE_POINT_WALLET = "delete from point_wallet";
    private static final String DELETE_COFFEE_MENU = "delete from coffee_menu";
    private static final String DELETE_MEMBER = "delete from member";
    private static final String INSERT_MEMBER = "insert into member (id, created_at, updated_at) values (1, now(), now())";

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
        jdbcTemplate.update(DELETE_POINT_HISTORY);
        jdbcTemplate.update(DELETE_COFFEE_ORDER);
        jdbcTemplate.update(DELETE_ORDER_OUTBOX);
        jdbcTemplate.update(DELETE_POINT_WALLET);
        jdbcTemplate.update(DELETE_COFFEE_MENU);
        jdbcTemplate.update(DELETE_MEMBER);

        jdbcTemplate.update(INSERT_MEMBER);
        clearLock();
    }

    @Test
    @DisplayName("같은 회원의 포인트 충전 요청이 동시에 들어와도 최종 잔액과 이력이 정합성을 유지한다")
    void chargeMaintainsConsistencyUnderConcurrentRequests() throws InterruptedException {
        int threadCount = 10;
        AtomicInteger successCount = new AtomicInteger();
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
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

            await(readyLatch);
            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
        }

        long balance = pointWalletRepository.findByMemberId(1L).orElseThrow().getBalance();
        long chargeHistoryCount = pointHistoryRepository.countByMemberIdAndType(1L, PointHistoryType.CHARGE);

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(balance).isEqualTo(10_000L);
        assertThat(chargeHistoryCount).isEqualTo(threadCount);
    }

    private void clearLock() {
        stringRedisTemplate.delete(POINT_LOCK_KEY);
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
