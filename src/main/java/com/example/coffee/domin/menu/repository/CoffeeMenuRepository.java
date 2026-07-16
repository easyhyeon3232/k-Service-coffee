package com.example.coffee.domin.menu.repository;

import com.example.coffee.domin.menu.entity.CoffeeMenu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 커피 메뉴 조회를 담당하는 리포지토리다.
 */
public interface CoffeeMenuRepository extends JpaRepository<CoffeeMenu, Long> {

    // 전체 메뉴를 ID 오름차순으로 조회한다.
    List<CoffeeMenu> findAllByOrderByIdAsc();
}
