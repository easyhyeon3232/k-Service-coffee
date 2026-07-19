package com.example.coffee.domin.order.repository;

import com.example.coffee.domin.order.entity.IdempotencyKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByRequestKey(String requestKey);
}
