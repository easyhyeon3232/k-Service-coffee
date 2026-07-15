package com.example.coffee.order.domain;

import com.example.coffee.common.entity.BaseTimeEntity;
import com.example.coffee.member.domain.Member;
import com.example.coffee.menu.domain.CoffeeMenu;
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

@Getter
@Entity
@Table(name = "coffee_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeeOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
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

    public static CoffeeOrder completed(Member member, CoffeeMenu menu, long orderPrice) {
        return new CoffeeOrder(member, menu, orderPrice);
    }
}
