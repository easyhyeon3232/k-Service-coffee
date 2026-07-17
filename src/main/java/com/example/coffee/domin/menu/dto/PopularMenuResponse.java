package com.example.coffee.domin.menu.dto;

/**
 * 최근 7일 기준 인기 메뉴 조회 응답 DTO다.
 *
 * @param menuId 메뉴 ID
 * @param name 메뉴명
 * @param price 메뉴 가격
 * @param orderCount 최근 7일 주문 횟수
 */
public record PopularMenuResponse(Long menuId, String name, long price, long orderCount) {
}
