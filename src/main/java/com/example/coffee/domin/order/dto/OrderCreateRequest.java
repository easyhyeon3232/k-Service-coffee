package com.example.coffee.domin.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long memberId,

        @NotNull(message = "메뉴 ID는 필수입니다.")
        Long menuId,

        @NotBlank(message = "idempotency key는 필수입니다.")
        String idempotencyKey
) {
}
