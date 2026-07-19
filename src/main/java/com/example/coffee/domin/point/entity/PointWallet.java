package com.example.coffee.domin.point.entity;

import com.example.coffee.common.entity.BaseTimeEntity;
import com.example.coffee.common.exception.BusinessException;
import com.example.coffee.common.exception.ErrorCode;
import com.example.coffee.domin.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원의 현재 포인트 잔액을 관리하는 엔티티다.
 */
@Getter
@Entity
@Table(
        name = "point_wallet",
        indexes = {
                @Index(name = "idx_point_wallet_member_id", columnList = "member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointWallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private long balance;

    private PointWallet(Member member, long balance) {
        this.member = member;
        this.balance = balance;
    }

    // 회원 전용 포인트 지갑을 생성한다.
    public static PointWallet create(Member member) {
        return new PointWallet(member, 0L);
    }

    // 포인트를 충전한다.
    public void charge(long amount) {
        validatePositiveAmount(amount);
        this.balance += amount;
    }

    // 포인트를 사용한다.
    public void use(long amount) {
        validatePositiveAmount(amount);
        if (balance < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.balance -= amount;
    }

    // 0 이하 금액 요청을 막는다.
    private void validatePositiveAmount(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT);
        }
    }
}