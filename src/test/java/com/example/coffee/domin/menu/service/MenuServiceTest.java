package com.example.coffee.domin.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

import com.example.coffee.common.exception.BusinessException;
import com.example.coffee.common.exception.ErrorCode;
import com.example.coffee.domin.menu.dto.MenuResponse;
import com.example.coffee.domin.menu.dto.PopularMenuResponse;
import com.example.coffee.domin.menu.entity.CoffeeMenu;
import com.example.coffee.domin.menu.entity.CoffeeMenuStatus;
import com.example.coffee.domin.menu.repository.CoffeeMenuRepository;
import com.example.coffee.domin.order.entity.OrderStatus;
import com.example.coffee.domin.order.repository.CoffeeOrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @InjectMocks
    private MenuService menuService;

    @Mock
    private CoffeeMenuRepository coffeeMenuRepository;

    @Mock
    private CoffeeOrderRepository coffeeOrderRepository;

    @Test
    @DisplayName("메뉴 목록 조회 시 메뉴 응답 DTO 목록을 반환한다")
    void getMenusReturnsMenuResponses() {
        CoffeeMenu americano = createMenu(1L, "Americano", 3000L);
        CoffeeMenu latte = createMenu(2L, "Latte", 4000L);

        given(coffeeMenuRepository.findByStatusOrderByIdAsc(CoffeeMenuStatus.ON_SALE)).willReturn(List.of(americano, latte));

        List<MenuResponse> response = menuService.getMenus();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).menuId()).isEqualTo(1L);
        assertThat(response.get(0).name()).isEqualTo("Americano");
        assertThat(response.get(1).menuId()).isEqualTo(2L);
        assertThat(response.get(1).name()).isEqualTo("Latte");
    }

    @Test
    @DisplayName("인기 메뉴 조회 시 최근 7일 기준 상위 3개 메뉴를 반환한다")
    void getPopularMenusReturnsTopThreeMenus() {
        List<PopularMenuResponse> popularMenus = List.of(
                new PopularMenuResponse(1L, "Americano", 3000L, 5L),
                new PopularMenuResponse(2L, "Latte", 4000L, 3L),
                new PopularMenuResponse(3L, "Mocha", 4500L, 2L)
        );

        given(coffeeOrderRepository.findPopularMenus(
                eq(OrderStatus.COMPLETED),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(popularMenus);

        List<PopularMenuResponse> response = menuService.getPopularMenus();

        assertThat(response).hasSize(3);
        assertThat(response.get(0).name()).isEqualTo("Americano");
        assertThat(response.get(0).orderCount()).isEqualTo(5L);
        assertThat(response.get(1).name()).isEqualTo("Latte");
        assertThat(response.get(2).name()).isEqualTo("Mocha");
    }

    @Test
    @DisplayName("인기 메뉴 조회 중 DB 예외가 발생하면 공통 비즈니스 예외로 변환한다")
    void getPopularMenusThrowsBusinessExceptionWhenRepositoryFails() {
        given(coffeeOrderRepository.findPopularMenus(
                eq(OrderStatus.COMPLETED),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willThrow(new DataAccessResourceFailureException("db error"));

        assertThatThrownBy(() -> menuService.getPopularMenus())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POPULAR_MENU_FETCH_FAILED);
    }

    private CoffeeMenu createMenu(Long menuId, String name, long price) {
        CoffeeMenu coffeeMenu = org.mockito.Mockito.mock(CoffeeMenu.class);
        lenient().when(coffeeMenu.getId()).thenReturn(menuId);
        lenient().when(coffeeMenu.getName()).thenReturn(name);
        lenient().when(coffeeMenu.getPrice()).thenReturn(price);
        return coffeeMenu;
    }
}
