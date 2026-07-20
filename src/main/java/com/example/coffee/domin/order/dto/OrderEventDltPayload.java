package com.example.coffee.domin.order.dto;

import java.time.LocalDateTime;

/**
 * DLT 토픽으로 이동한 주문 이벤트의 실패 메타데이터를 담는 payload다.
 */
public record OrderEventDltPayload(
        String originalTopic,
        int originalPartition,
        long originalOffset,
        String originalKey,
        String originalPayload,
        String failureReason,
        int retryCount,
        LocalDateTime occurredAt
) {
}
