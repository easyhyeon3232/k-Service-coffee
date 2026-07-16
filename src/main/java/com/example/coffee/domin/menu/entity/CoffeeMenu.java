package com.example.coffee.domin.menu.entity;

import com.example.coffee.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 가능한 커피 메뉴를 표현하는 엔티티다.
 */
@Getter
@Entity
@Table(name = "coffee_menu")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeeMenu extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoffeeMenuStatus status;

    private CoffeeMenu(String name, long price, CoffeeMenuStatus status) {
        this.name = name;
        this.price = price;
        this.status = status;
    }

    // 테스트나 초기 데이터 구성 시 사용할 메뉴 엔티티를 생성한다.
    public static CoffeeMenu create(String name, long price, CoffeeMenuStatus status) {
        return new CoffeeMenu(name, price, status);
    }

    // 현재 메뉴가 주문 가능한 상태인지 확인한다.
    public boolean isOrderable() {
        return status == CoffeeMenuStatus.ON_SALE;
    }
}
