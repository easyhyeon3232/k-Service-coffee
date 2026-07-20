package com.example.coffee.domin.order.entity;

import com.example.coffee.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Kafka consumer가 처리한 주문 이벤트를 기록해 중복 소비를 방지하는 엔티티다.
 */
@Getter
@Entity
@Table(
        name = "order_event_consume_log",
        indexes = {
                @Index(name = "idx_order_event_consume_log_partition_offset", columnList = "topic_partition, topic_offset")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEventConsumeLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false, length = 100)
    private String consumerGroup;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false)
    private int topicPartition;

    @Column(nullable = false)
    private long topicOffset;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    private OrderEventConsumeLog(
            Long orderId,
            String consumerGroup,
            String topic,
            int topicPartition,
            long topicOffset,
            String payload
    ) {
        this.orderId = orderId;
        this.consumerGroup = consumerGroup;
        this.topic = topic;
        this.topicPartition = topicPartition;
        this.topicOffset = topicOffset;
        this.payload = payload;
    }

    // 처리 완료된 Kafka 주문 이벤트를 소비 로그로 남긴다.
    public static OrderEventConsumeLog create(
            Long orderId,
            String consumerGroup,
            String topic,
            int topicPartition,
            long topicOffset,
            String payload
    ) {
        return new OrderEventConsumeLog(orderId, consumerGroup, topic, topicPartition, topicOffset, payload);
    }
}
