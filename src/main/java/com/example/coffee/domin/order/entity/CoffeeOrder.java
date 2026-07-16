package com.example.coffee.domin.order.entity;

import com.example.coffee.common.entity.BaseTimeEntity;
import com.example.coffee.domin.member.entity.Member;
import com.example.coffee.domin.menu.entity.CoffeeMenu;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 커피 주문과 결제 결과를 저장하는 주문 엔티티다.
 */
@Getter
@Entity
@Table(name = "coffee_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeeOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private CoffeeMenu menu;

    @Column(nullable = false)
    private long orderPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private CoffeeOrder(Member member, CoffeeMenu menu, long orderPrice) {
        this.member = member;
        this.menu = menu;
        this.orderPrice = orderPrice;
        this.status = OrderStatus.COMPLETED;
        this.orderedAt = LocalDateTime.now();
    }

    // 완료된 주문 엔티티를 생성한다.
    public static CoffeeOrder completed(Member member, CoffeeMenu menu, long orderPrice) {
        return new CoffeeOrder(member, menu, orderPrice);
    }
}
