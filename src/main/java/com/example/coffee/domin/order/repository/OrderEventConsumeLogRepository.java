package com.example.coffee.domin.order.repository;

import com.example.coffee.domin.order.entity.OrderEventConsumeLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventConsumeLogRepository extends JpaRepository<OrderEventConsumeLog, Long> {

    boolean existsByOrderId(Long orderId);
}
