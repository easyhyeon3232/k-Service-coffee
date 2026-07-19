package com.example.coffee.domin.menu.service;

import com.example.coffee.domin.menu.dto.PopularMenuResponse;
import com.example.coffee.domin.menu.entity.CoffeeMenu;
import com.example.coffee.domin.menu.repository.CoffeeMenuRepository;
import com.example.coffee.domin.order.entity.CoffeeOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

/**
 * Redis ZSET과 요약 캐시를 사용해 인기 메뉴 조회를 최적화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularMenuCacheService {

    private static final String POPULAR_MENU_CACHE_KEY = "cache:popular:menus";
    private static final String DAILY_RANKING_KEY_PREFIX = "popular:menu:";
    private static final String ORDER_MARKER_KEY_PREFIX = "popular:menu:recorded:order:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final TypeReference<List<PopularMenuResponse>> POPULAR_MENU_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate stringRedisTemplate;
    private final CoffeeMenuRepository coffeeMenuRepository;
    private final ObjectMapper objectMapper;

    @Value("${popular-menu.cache.ttl-seconds:300}")
    private long popularMenuCacheTtlSeconds;

    @Value("${popular-menu.daily-key.ttl-days:8}")
    private long dailyKeyTtlDays;

    // 요약 캐시나 ZSET 집계 결과가 있으면 인기 메뉴를 반환한다.
    public Optional<List<PopularMenuResponse>> getPopularMenus() {
        Optional<List<PopularMenuResponse>> cachedMenus = readSummaryCache();
        if (cachedMenus.isPresent()) {
            return cachedMenus;
        }

        List<PopularMenuResponse> zsetMenus = buildFromZSet();
        if (zsetMenus.isEmpty()) {
            return Optional.empty();
        }

        cachePopularMenus(zsetMenus);
        return Optional.of(zsetMenus);
    }

    // DB 집계 결과를 요약 캐시에 저장한다.
    public void cachePopularMenus(List<PopularMenuResponse> popularMenus) {
        try {
            stringRedisTemplate.opsForValue().set(
                    POPULAR_MENU_CACHE_KEY,
                    objectMapper.writeValueAsString(popularMenus),
                    Duration.ofSeconds(popularMenuCacheTtlSeconds)
            );
        } catch (JsonProcessingException exception) {
            log.warn("인기 메뉴 요약 캐시 직렬화에 실패했습니다.", exception);
        }
    }

    // 주문 완료 건을 하루 단위 ZSET에 반영하고 기존 요약 캐시를 비운다.
    public void recordOrder(CoffeeOrder coffeeOrder) {
        String markerKey = ORDER_MARKER_KEY_PREFIX + coffeeOrder.getId();
        Boolean firstRecord = stringRedisTemplate.opsForValue().setIfAbsent(
                markerKey,
                "1",
                Duration.ofDays(dailyKeyTtlDays)
        );

        if (!Boolean.TRUE.equals(firstRecord)) {
            return;
        }

        String rankingKey = DAILY_RANKING_KEY_PREFIX + formatDate(coffeeOrder.getOrderedAt().toLocalDate());
        stringRedisTemplate.opsForZSet().incrementScore(rankingKey, String.valueOf(coffeeOrder.getMenu().getId()), 1D);
        stringRedisTemplate.expire(rankingKey, Duration.ofDays(dailyKeyTtlDays));
        stringRedisTemplate.delete(POPULAR_MENU_CACHE_KEY);
    }

    private Optional<List<PopularMenuResponse>> readSummaryCache() {
        String cachedValue = stringRedisTemplate.opsForValue().get(POPULAR_MENU_CACHE_KEY);
        if (cachedValue == null || cachedValue.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(cachedValue, POPULAR_MENU_TYPE));
        } catch (JsonProcessingException exception) {
            log.warn("인기 메뉴 요약 캐시 역직렬화에 실패했습니다.", exception);
            return Optional.empty();
        }
    }

    private List<PopularMenuResponse> buildFromZSet() {
        Map<Long, Long> aggregatedScores = new HashMap<>();

        for (int offset = 0; offset < 7; offset++) {
            String dailyKey = DAILY_RANKING_KEY_PREFIX + formatDate(LocalDate.now().minusDays(offset));
            Set<TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(dailyKey, 0, -1);
            if (tuples == null) {
                continue;
            }

            for (TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null || tuple.getScore() == null) {
                    continue;
                }

                long menuId = Long.parseLong(tuple.getValue());
                long score = tuple.getScore().longValue();
                aggregatedScores.merge(menuId, score, Long::sum);
            }
        }

        if (aggregatedScores.isEmpty()) {
            return List.of();
        }

        List<Long> topMenuIds = aggregatedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, CoffeeMenu> menusById = new HashMap<>();
        for (CoffeeMenu coffeeMenu : coffeeMenuRepository.findAllById(topMenuIds)) {
            menusById.put(coffeeMenu.getId(), coffeeMenu);
        }

        List<PopularMenuResponse> responses = new ArrayList<>();
        for (Long menuId : topMenuIds) {
            CoffeeMenu coffeeMenu = menusById.get(menuId);
            if (coffeeMenu == null) {
                continue;
            }

            responses.add(new PopularMenuResponse(
                    coffeeMenu.getId(),
                    coffeeMenu.getName(),
                    coffeeMenu.getPrice(),
                    aggregatedScores.getOrDefault(menuId, 0L)
            ));
        }

        return responses.stream()
                .sorted(Comparator.comparingLong(PopularMenuResponse::orderCount).reversed()
                        .thenComparing(PopularMenuResponse::menuId))
                .collect(Collectors.toList());
    }

    private String formatDate(LocalDate localDate) {
        return localDate.format(DATE_FORMATTER);
    }
}
