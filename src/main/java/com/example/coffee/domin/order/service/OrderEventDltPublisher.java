package com.example.coffee.domin.order.service;

import com.example.coffee.domin.order.dto.OrderEventDltPayload;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 재시도에 실패했거나 복구 불가능한 주문 이벤트를 DLT 토픽으로 전송하는 컴포넌트다.
 */
@Component
@RequiredArgsConstructor
public class OrderEventDltPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderEventDltPayloadMapper orderEventDltPayloadMapper;

    @Value("${order-event.kafka.dlt-topic}")
    private String dltTopic;

    @Value("${order-event.kafka.consumer.retry-max-attempts:3}")
    private int retryMaxAttempts;

    // 실패한 주문 이벤트를 DLT payload로 감싸 지정된 DLT 토픽으로 전송한다.
    public void publish(ConsumerRecord<?, ?> record, Exception exception) {
        OrderEventDltPayload dltPayload = new OrderEventDltPayload(
                record.topic(),
                record.partition(),
                record.offset(),
                record.key() == null ? null : String.valueOf(record.key()),
                record.value() == null ? null : String.valueOf(record.value()),
                buildFailureReason(exception),
                resolveRetryCount(exception),
                LocalDateTime.now()
        );

        kafkaTemplate.send(dltTopic, dltPayload.originalKey(), orderEventDltPayloadMapper.toJson(dltPayload)).join();
    }

    private int resolveRetryCount(Exception exception) {
        if (isUnrecoverable(exception)) {
            return 0;
        }
        return retryMaxAttempts;
    }

    private boolean isUnrecoverable(Exception exception) {
        return exception instanceof OrderEventUnrecoverableException
                || exception instanceof IllegalArgumentException;
    }

    private String buildFailureReason(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + ": " + message;
    }
}
