package com.example.coffee.domin.menu.service;

import com.example.coffee.common.exception.BusinessException;
import com.example.coffee.common.exception.ErrorCode;
import com.example.coffee.domin.menu.dto.MenuResponse;
import com.example.coffee.domin.menu.repository.CoffeeMenuRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커피 메뉴 조회 기능을 담당하는 서비스다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final CoffeeMenuRepository coffeeMenuRepository;

    // 저장된 전체 메뉴를 조회해 메뉴 목록 응답으로 변환한다.
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenus() {
        try {
            return coffeeMenuRepository.findAllByOrderByIdAsc().stream()
                    .map(MenuResponse::from)
                    .toList();
        } catch (DataAccessException exception) {
            log.error("메뉴 목록 조회 중 DB 예외가 발생했습니다.", exception);
            throw new BusinessException(ErrorCode.MENU_LIST_FETCH_FAILED);
        }
    }
}
