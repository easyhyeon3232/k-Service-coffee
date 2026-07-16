package com.example.coffee.domin.order.entity;

import com.example.coffee.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 정보를 외부 데이터 수집 플랫폼으로 전달하기 위한 outbox 엔티티다.
 */
@Getter
@Entity
@Table(name = "order_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderOutbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CoffeeOrder order;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    private LocalDateTime lastAttemptAt;

    private OrderOutbox(CoffeeOrder order, String payload) {
        this.order = order;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
    }

    // 전송 대기 상태의 outbox 이벤트를 생성한다.
    public static OrderOutbox pending(CoffeeOrder order, String payload) {
        return new OrderOutbox(order, payload);
    }

    // 전송 성공 상태로 변경한다.
    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.lastAttemptAt = LocalDateTime.now();
    }

    // 전송 실패 상태로 변경하고 재시도 횟수를 증가시킨다.
    public void markFailed() {
        this.status = OutboxStatus.FAILED;
        this.retryCount += 1;
        this.lastAttemptAt = LocalDateTime.now();
    }
}
