package com.example.coffee.domin.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.coffee.domin.menu.dto.PopularMenuResponse;
import com.example.coffee.domin.menu.repository.CoffeeMenuRepository;
import com.example.coffee.domin.order.entity.CoffeeOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PopularMenuCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private CoffeeMenuRepository coffeeMenuRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private PopularMenuCacheService popularMenuCacheService;

    @BeforeEach
    void setUp() {
        popularMenuCacheService = new PopularMenuCacheService(
                stringRedisTemplate,
                coffeeMenuRepository,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(popularMenuCacheService, "popularMenuCacheTtlSeconds", 300L);
        ReflectionTestUtils.setField(popularMenuCacheService, "dailyKeyTtlDays", 8L);
    }

    @Test
    @DisplayName("요약 캐시가 있으면 인기 메뉴 목록을 바로 반환한다")
    void getPopularMenusReturnsSummaryCache() throws Exception {
        List<PopularMenuResponse> popularMenus = List.of(
                new PopularMenuResponse(1L, "Americano", 3000L, 5L),
                new PopularMenuResponse(2L, "Latte", 4000L, 3L)
        );

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("cache:popular:menus"))
                .willReturn(new ObjectMapper().writeValueAsString(popularMenus));

        Optional<List<PopularMenuResponse>> result = popularMenuCacheService.getPopularMenus();

        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).hasSize(2);
        assertThat(result.orElseThrow().getFirst().name()).isEqualTo("Americano");
    }

    @Test
    @DisplayName("주문 반영 시 일자별 ZSET 점수를 올리고 요약 캐시를 비운다")
    void recordOrderIncrementsDailyScoreAndEvictsSummaryCache() {
        CoffeeOrder coffeeOrder = org.mockito.Mockito.mock(CoffeeOrder.class);
        com.example.coffee.domin.menu.entity.CoffeeMenu coffeeMenu =
                org.mockito.Mockito.mock(com.example.coffee.domin.menu.entity.CoffeeMenu.class);

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(coffeeOrder.getId()).willReturn(10L);
        given(coffeeOrder.getOrderedAt()).willReturn(LocalDateTime.of(2026, 7, 19, 10, 0));
        given(coffeeOrder.getMenu()).willReturn(coffeeMenu);
        given(coffeeMenu.getId()).willReturn(1L);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(true);

        popularMenuCacheService.recordOrder(coffeeOrder);

        verify(zSetOperations).incrementScore("popular:menu:20260719", "1", 1D);
        verify(stringRedisTemplate).delete("cache:popular:menus");
    }
}
