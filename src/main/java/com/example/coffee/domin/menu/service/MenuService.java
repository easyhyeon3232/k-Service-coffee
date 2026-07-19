package com.example.coffee.domin.menu.service;

import com.example.coffee.common.exception.BusinessException;
import com.example.coffee.common.exception.ErrorCode;
import com.example.coffee.domin.menu.dto.MenuResponse;
import com.example.coffee.domin.menu.dto.PopularMenuResponse;
import com.example.coffee.domin.menu.entity.CoffeeMenuStatus;
import com.example.coffee.domin.menu.repository.CoffeeMenuRepository;
import com.example.coffee.domin.order.entity.OrderStatus;
import com.example.coffee.domin.order.repository.CoffeeOrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커피 메뉴 조회와 인기 메뉴 집계를 담당하는 서비스다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final CoffeeMenuRepository coffeeMenuRepository;
    private final CoffeeOrderRepository coffeeOrderRepository;
    private final PopularMenuCacheService popularMenuCacheService;

    // 판매 중인 메뉴 목록을 캐시와 함께 조회해 응답 DTO로 변환한다.
    @Cacheable(cacheNames = "menus", key = "'on-sale'")
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenus() {
        try {
            return coffeeMenuRepository.findByStatusOrderByIdAsc(CoffeeMenuStatus.ON_SALE).stream()
                    .map(MenuResponse::from)
                    .toList();
        } catch (DataAccessException exception) {
            log.error("메뉴 목록 조회 중 DB 예외가 발생했습니다.", exception);
            throw new BusinessException(ErrorCode.MENU_LIST_FETCH_FAILED);
        }
    }

    // 최근 7일 기준 인기 메뉴 3개를 Redis 우선 조회 후, 없으면 DB에서 집계한다.
    @Transactional(readOnly = true)
    public List<PopularMenuResponse> getPopularMenus() {
        try {
            return popularMenuCacheService.getPopularMenus()
                    .orElseGet(this::getPopularMenusFromDatabase);
        } catch (DataAccessException exception) {
            log.error("인기 메뉴 조회 중 DB 예외가 발생했습니다.", exception);
            throw new BusinessException(ErrorCode.POPULAR_MENU_FETCH_FAILED);
        }
    }

    private List<PopularMenuResponse> getPopularMenusFromDatabase() {
        List<PopularMenuResponse> popularMenus = coffeeOrderRepository.findPopularMenus(
                OrderStatus.COMPLETED,
                LocalDateTime.now().minusDays(7),
                PageRequest.of(0, 3)
        );
        popularMenuCacheService.cachePopularMenus(popularMenus);
        return popularMenus;
    }
}
