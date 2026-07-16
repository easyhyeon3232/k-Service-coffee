package com.example.coffee.domin.order.repository;

import com.example.coffee.domin.order.entity.OrderOutbox;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, Long> {

    Optional<OrderOutbox> findByOrderId(Long orderId);
}
