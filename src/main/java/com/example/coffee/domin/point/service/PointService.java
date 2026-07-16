package com.example.coffee.domin.point.service;

import com.example.coffee.common.exception.BusinessException;
import com.example.coffee.common.exception.ErrorCode;
import com.example.coffee.domin.member.entity.Member;
import com.example.coffee.domin.member.repository.MemberRepository;
import com.example.coffee.domin.point.dto.PointChargeResponse;
import com.example.coffee.domin.point.entity.PointHistory;
import com.example.coffee.domin.point.entity.PointWallet;
import com.example.coffee.domin.point.repository.PointHistoryRepository;
import com.example.coffee.domin.point.repository.PointWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 충전과 충전 이력 저장을 담당하는 서비스다.
 */
@Service
@RequiredArgsConstructor
public class PointService {

    private final MemberRepository memberRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 회원 포인트를 충전하고 충전 이력을 남긴다.
    @Transactional
    public PointChargeResponse charge(Long memberId, long amount) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            PointWallet pointWallet = pointWalletRepository.findByMemberId(memberId)
                    .orElseGet(() -> pointWalletRepository.save(PointWallet.create(member)));

            pointWallet.charge(amount);
            PointWallet savedPointWallet = pointWalletRepository.save(pointWallet);
            pointHistoryRepository.save(PointHistory.charge(member, amount, savedPointWallet.getBalance()));

            return PointChargeResponse.of(savedPointWallet, amount);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new BusinessException(ErrorCode.POINT_CHARGE_CONFLICT);
        }
    }
}
