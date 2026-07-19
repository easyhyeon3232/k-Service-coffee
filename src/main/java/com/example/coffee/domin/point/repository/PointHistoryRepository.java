package com.example.coffee.domin.point.repository;

import com.example.coffee.domin.point.entity.PointHistory;
import com.example.coffee.domin.point.entity.PointHistoryType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    long countByMemberIdAndType(Long memberId, PointHistoryType type);
}
