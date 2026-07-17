package com.example.coffee.domin.menu.controller;

import com.example.coffee.common.response.CommonResponse;
import com.example.coffee.domin.menu.dto.MenuResponse;
import com.example.coffee.domin.menu.dto.PopularMenuResponse;
import com.example.coffee.domin.menu.service.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 커피 메뉴 관련 조회 API를 처리하는 컨트롤러다.
 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // 전체 커피 메뉴 목록을 조회한다.
    @GetMapping
    public ResponseEntity<CommonResponse<List<MenuResponse>>> getMenus() {
        return ResponseEntity.ok(CommonResponse.success(menuService.getMenus()));
    }

    // 최근 7일 기준 인기 메뉴 3개를 조회한다.
    @GetMapping("/popular")
    public ResponseEntity<CommonResponse<List<PopularMenuResponse>>> getPopularMenus() {
        return ResponseEntity.ok(CommonResponse.success(menuService.getPopularMenus()));
    }
}
