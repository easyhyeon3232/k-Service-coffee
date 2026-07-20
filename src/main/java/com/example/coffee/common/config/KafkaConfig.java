package com.example.coffee.common.config;

import com.example.coffee.domin.order.service.OrderEventDltPublisher;
import com.example.coffee.domin.order.service.OrderEventUnrecoverableException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * 주문 이벤트 토픽과 Kafka consumer listener 공통 설정을 담당한다.
 */
@Configuration
public class KafkaConfig {

    @Value("${order-event.kafka.topic}")
    private String topic;

    @Value("${order-event.kafka.dlt-topic}")
    private String dltTopic;

    @Value("${order-event.kafka.partitions:3}")
    private int partitions;

    @Value("${order-event.kafka.replication-factor:1}")
    private short replicationFactor;

    @Value("${order-event.kafka.consumer.retry-interval-ms:1000}")
    private long retryIntervalMs;

    @Value("${order-event.kafka.consumer.retry-max-attempts:3}")
    private long retryMaxAttempts;

    // 주문 이벤트 토픽을 애플리케이션 시작 시 자동으로 생성한다.
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    // DLT 토픽을 자동 생성해 소비 실패 메시지를 정상 흐름과 분리한다.
    @Bean
    public NewTopic orderCreatedDltTopic() {
        return TopicBuilder.name(dltTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    // consumer가 처리 완료 이후에만 offset을 커밋하도록 수동 ack 모드로 설정한다.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    // 복구 불가능한 예외는 즉시 DLT로 보내고, 그 외 예외는 재시도 후 DLT로 분리한다.
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(OrderEventDltPublisher orderEventDltPublisher) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                orderEventDltPublisher::publish,
                new FixedBackOff(retryIntervalMs, retryMaxAttempts - 1)
        );
        errorHandler.addNotRetryableExceptions(
                OrderEventUnrecoverableException.class,
                IllegalArgumentException.class
        );
        return errorHandler;
    }
}
