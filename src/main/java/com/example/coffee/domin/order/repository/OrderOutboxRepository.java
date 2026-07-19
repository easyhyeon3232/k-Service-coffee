package com.example.coffee.domin.order.repository;

import com.example.coffee.domin.order.entity.OrderOutbox;
import com.example.coffee.domin.order.entity.OutboxStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, Long> {

    Optional<OrderOutbox> findByOrderId(Long orderId);

    List<OrderOutbox> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
