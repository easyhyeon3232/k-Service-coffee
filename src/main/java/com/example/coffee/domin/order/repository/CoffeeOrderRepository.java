package com.example.coffee.domin.order.repository;

import com.example.coffee.domin.menu.dto.PopularMenuResponse;
import com.example.coffee.domin.order.entity.CoffeeOrder;
import com.example.coffee.domin.order.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoffeeOrderRepository extends JpaRepository<CoffeeOrder, Long> {

    @Query("""
            select new com.example.coffee.domin.menu.dto.PopularMenuResponse(
                menu.id,
                menu.name,
                menu.price,
                count(coffeeOrder.id)
            )
            from CoffeeOrder coffeeOrder
            join coffeeOrder.menu menu
            where coffeeOrder.status = :status
              and coffeeOrder.orderedAt >= :since
            group by menu.id, menu.name, menu.price
            order by count(coffeeOrder.id) desc, menu.id asc
            """)
    List<PopularMenuResponse> findPopularMenus(
            @Param("status") OrderStatus status,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );

    long countByMemberIdAndStatus(Long memberId, OrderStatus status);
}
