package com.example.coffee.domin.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.coffee.common.exception.BusinessException;
import com.example.coffee.common.exception.ErrorCode;
import com.example.coffee.domin.order.entity.OrderStatus;
import com.example.coffee.domin.order.repository.CoffeeOrderRepository;
import com.example.coffee.domin.point.entity.PointHistoryType;
import com.example.coffee.domin.point.repository.PointHistoryRepository;
import com.example.coffee.domin.point.repository.PointWalletRepository;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
class OrderDistributedLockIntegrationTest {

    private static final String ORDER_LOCK_KEY = "lock:order:member:1";
    private static final String DELETE_POINT_HISTORY = "delete from point_history";
    private static final String DELETE_COFFEE_ORDER = "delete from coffee_order";
    private static final String DELETE_ORDER_OUTBOX = "delete from order_outbox";
    private static final String DELETE_POINT_WALLET = "delete from point_wallet";
    private static final String DELETE_COFFEE_MENU = "delete from coffee_menu";
    private static final String DELETE_IDEMPOTENCY_KEY = "delete from idempotency_key";
    private static final String DELETE_MEMBER = "delete from member";
    private static final String INSERT_MEMBER = "insert into member (id, created_at, updated_at) values (1, now(), now())";
    private static final String INSERT_MENU = """
            insert into coffee_menu (id, name, price, status, created_at, updated_at)
            values (1, 'Americano', 3000, 'ON_SALE', now(), now())
            """;
    private static final String INSERT_WALLET = """
            insert into point_wallet (id, member_id, version, balance, created_at, updated_at)
            values (1, 1, 0, 3000, now(), now())
            """;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PointWalletRepository pointWalletRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private CoffeeOrderRepository coffeeOrderRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(DELETE_POINT_HISTORY);
        jdbcTemplate.update(DELETE_COFFEE_ORDER);
        jdbcTemplate.update(DELETE_ORDER_OUTBOX);
        jdbcTemplate.update(DELETE_POINT_WALLET);
        jdbcTemplate.update(DELETE_COFFEE_MENU);
        jdbcTemplate.update(DELETE_IDEMPOTENCY_KEY);
        jdbcTemplate.update(DELETE_MEMBER);

        jdbcTemplate.update(INSERT_MEMBER);
        jdbcTemplate.update(INSERT_MENU);
        jdbcTemplate.update(INSERT_WALLET);
        clearLock();
    }

    @Test
    @DisplayName("같은 회원의 주문 요청이 동시에 들어와도 한 번만 결제되고 잔액은 음수가 되지 않는다")
    void orderMaintainsConsistencyUnderConcurrentRequests() throws InterruptedException {
        int threadCount = 5;
        AtomicInteger successCount = new AtomicInteger();
        List<ErrorCode> failureCodes = new CopyOnWriteArrayList<>();
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int index = 0; index < threadCount; index++) {
                final int requestNumber = index;
                executorService.submit(() -> {
                    readyLatch.countDown();
                    await(startLatch);
                    try {
                        orderFacade.order(1L, 1L, "order-lock-test-" + requestNumber);
                        successCount.incrementAndGet();
                    } catch (BusinessException exception) {
                        failureCodes.add(exception.getErrorCode());
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
        long orderCount = coffeeOrderRepository.countByMemberIdAndStatus(1L, OrderStatus.COMPLETED);
        long useHistoryCount = pointHistoryRepository.countByMemberIdAndType(1L, PointHistoryType.USE);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCodes).hasSize(threadCount - 1);
        assertThat(failureCodes).allMatch(errorCode -> errorCode == ErrorCode.INSUFFICIENT_POINT);
        assertThat(balance).isZero();
        assertThat(orderCount).isEqualTo(1L);
        assertThat(useHistoryCount).isEqualTo(1L);
    }

    private void clearLock() {
        stringRedisTemplate.delete(ORDER_LOCK_KEY);
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
