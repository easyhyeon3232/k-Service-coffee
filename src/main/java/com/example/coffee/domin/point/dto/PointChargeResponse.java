package com.example.coffee.domin.point.dto;

import com.example.coffee.domin.point.entity.PointWallet;

public record PointChargeResponse(
        Long memberId,
        long chargedAmount,
        long balance
) {

    // 충전 결과를 응답 DTO로 변환한다.
    public static PointChargeResponse of(PointWallet pointWallet, long chargedAmount) {
        return new PointChargeResponse(
                pointWallet.getMember().getId(),
                chargedAmount,
                pointWallet.getBalance()
        );
    }
}
