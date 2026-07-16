package com.example.coffee.domin.menu.dto;

import com.example.coffee.domin.menu.entity.CoffeeMenu;

/**
 * 커피 메뉴 목록 조회 응답 DTO다.
 *
 * @param menuId 메뉴 ID
 * @param name 메뉴명
 * @param price 메뉴 가격
 */
public record MenuResponse(Long menuId, String name, long price) {

    // 메뉴 엔티티를 목록 조회 응답 DTO로 변환한다.
    public static MenuResponse from(CoffeeMenu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice()
        );
    }
}
