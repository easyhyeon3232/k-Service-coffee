package com.example.coffee.domin.order.entity;

import com.example.coffee.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 중복 요청 방지를 위한 idempotency key 정보를 저장하는 엔티티다.
 */
@Getter
@Entity
@Table(name = "idempotency_key")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String requestKey;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long menuId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", unique = true)
    private CoffeeOrder order;

    private IdempotencyKey(String requestKey, Long memberId, Long menuId) {
        this.requestKey = requestKey;
        this.memberId = memberId;
        this.menuId = menuId;
    }

    // 신규 주문 요청의 idempotency key를 예약 상태로 생성한다.
    public static IdempotencyKey reserve(String requestKey, Long memberId, Long menuId) {
        return new IdempotencyKey(requestKey, memberId, menuId);
    }

    // 주문이 완료되면 연결된 주문 정보를 저장한다.
    public void complete(CoffeeOrder order) {
        this.order = order;
    }

    // 같은 요청 정보로 들어온 idempotency key인지 확인한다.
    public boolean matches(Long memberId, Long menuId) {
        return this.memberId.equals(memberId) && this.menuId.equals(menuId);
    }

    // 이미 주문 결과가 연결된 key인지 확인한다.
    public boolean isCompleted() {
        return order != null;
    }
}
