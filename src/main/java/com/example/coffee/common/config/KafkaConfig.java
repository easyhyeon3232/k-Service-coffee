package com.example.coffee.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * 주문 이벤트 토픽과 Kafka consumer listener 공통 설정을 담당한다.
 */
@Configuration
public class KafkaConfig {

    @Value("${order-event.kafka.topic}")
    private String topic;

    @Value("${order-event.kafka.partitions:3}")
    private int partitions;

    @Value("${order-event.kafka.replication-factor:1}")
    private short replicationFactor;

    // 주문 이벤트 토픽을 애플리케이션 시작 시 자동으로 생성한다.
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    // consumer가 처리 완료 후에만 offset을 커밋하도록 수동 ack 모드로 설정한다.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
