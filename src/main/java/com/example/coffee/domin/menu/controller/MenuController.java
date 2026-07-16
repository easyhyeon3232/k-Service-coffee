package com.example.coffee.domin.menu.controller;

import com.example.coffee.common.response.CommonResponse;
import com.example.coffee.domin.menu.dto.MenuResponse;
import com.example.coffee.domin.menu.service.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 커피 메뉴 조회 API를 제공하는 컨트롤러다.
 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // 저장된 전체 커피 메뉴 목록을 조회한다.
    @GetMapping
    public ResponseEntity<CommonResponse<List<MenuResponse>>> getMenus() {
        return ResponseEntity.ok(CommonResponse.success(menuService.getMenus()));
    }
}
