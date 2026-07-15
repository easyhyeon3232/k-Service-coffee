package com.example.coffee.point.domain;

import com.example.coffee.member.domain.Member;
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
@Table(name = "point_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointHistoryType type;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private long balanceAfter;

    private Long referenceOrderId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private PointHistory(Member member, PointHistoryType type, long amount, long balanceAfter, Long referenceOrderId) {
        this.member = member;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceOrderId = referenceOrderId;
        this.createdAt = LocalDateTime.now();
    }

    public static PointHistory charge(Member member, long amount, long balanceAfter) {
        return new PointHistory(member, PointHistoryType.CHARGE, amount, balanceAfter, null);
    }

    public static PointHistory use(Member member, long amount, long balanceAfter, Long referenceOrderId) {
        return new PointHistory(member, PointHistoryType.USE, amount, balanceAfter, referenceOrderId);
    }
}
