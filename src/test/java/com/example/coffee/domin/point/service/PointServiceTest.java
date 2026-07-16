package com.example.coffee.domin.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.coffee.common.exception.BusinessException;
import com.example.coffee.common.exception.ErrorCode;
import com.example.coffee.domin.member.entity.Member;
import com.example.coffee.domin.member.repository.MemberRepository;
import com.example.coffee.domin.point.dto.PointChargeResponse;
import com.example.coffee.domin.point.entity.PointHistory;
import com.example.coffee.domin.point.entity.PointWallet;
import com.example.coffee.domin.point.repository.PointHistoryRepository;
import com.example.coffee.domin.point.repository.PointWalletRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PointWalletRepository pointWalletRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Test
    @DisplayName("지갑이 없는 회원은 1포인트 충전 시 지갑이 생성되고 충전 이력이 저장된다")
    void chargeCreatesWalletAndHistoryWhenWalletDoesNotExist() {
        Member member = createMemberMock(1L);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(pointWalletRepository.findByMemberId(1L)).willReturn(Optional.empty());
        given(pointWalletRepository.save(any(PointWallet.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(pointHistoryRepository.save(any(PointHistory.class))).willAnswer(invocation -> invocation.getArgument(0));

        PointChargeResponse response = pointService.charge(1L, 1L);

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.chargedAmount()).isEqualTo(1L);
        assertThat(response.balance()).isEqualTo(1L);

        verify(pointWalletRepository, times(2)).save(any(PointWallet.class));
        verify(pointHistoryRepository).save(any(PointHistory.class));
    }

    @Test
    @DisplayName("기존 지갑이 있는 회원은 충전 금액만큼 잔액이 증가한다")
    void chargeAddsBalanceWhenWalletExists() {
        Member member = createMemberMock(1L);
        PointWallet pointWallet = PointWallet.create(member);
        pointWallet.charge(500L);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(pointWalletRepository.findByMemberId(1L)).willReturn(Optional.of(pointWallet));
        given(pointWalletRepository.save(any(PointWallet.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(pointHistoryRepository.save(any(PointHistory.class))).willAnswer(invocation -> invocation.getArgument(0));

        PointChargeResponse response = pointService.charge(1L, 1000L);

        assertThat(response.balance()).isEqualTo(1500L);
        verify(pointWalletRepository, times(1)).save(pointWallet);
        verify(pointHistoryRepository).save(any(PointHistory.class));
    }

    @Test
    @DisplayName("충전 금액이 0원이면 예외가 발생한다")
    void chargeFailsWhenAmountIsZero() {
        Member member = createMember();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(pointWalletRepository.findByMemberId(1L)).willReturn(Optional.of(PointWallet.create(member)));

        assertThatThrownBy(() -> pointService.charge(1L, 0L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CHARGE_AMOUNT);
    }

    @Test
    @DisplayName("충전 금액이 음수면 예외가 발생한다")
    void chargeFailsWhenAmountIsNegative() {
        Member member = createMember();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(pointWalletRepository.findByMemberId(1L)).willReturn(Optional.of(PointWallet.create(member)));

        assertThatThrownBy(() -> pointService.charge(1L, -1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CHARGE_AMOUNT);
    }

    @Test
    @DisplayName("존재하지 않는 회원에게 충전하면 예외가 발생한다")
    void chargeFailsWhenMemberDoesNotExist() {
        given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> pointService.charge(99L, 1000L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("낙관적 락 충돌이 발생하면 충전 충돌 예외가 발생한다")
    void chargeFailsWhenOptimisticLockConflictOccurs() {
        Member member = createMember();
        PointWallet pointWallet = PointWallet.create(member);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(pointWalletRepository.findByMemberId(1L)).willReturn(Optional.of(pointWallet));
        given(pointWalletRepository.save(any(PointWallet.class)))
                .willThrow(new ObjectOptimisticLockingFailureException(PointWallet.class, 1L));

        assertThatThrownBy(() -> pointService.charge(1L, 1000L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POINT_CHARGE_CONFLICT);
    }

    private Member createMemberMock(Long memberId) {
        Member member = org.mockito.Mockito.mock(Member.class);
        given(member.getId()).willReturn(memberId);
        return member;
    }

    private Member createMember() {
        return org.mockito.Mockito.mock(Member.class);
    }
}
