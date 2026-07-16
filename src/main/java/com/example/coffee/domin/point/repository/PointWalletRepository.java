package com.example.coffee.domin.point.repository;

import com.example.coffee.domin.point.entity.PointWallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointWalletRepository extends JpaRepository<PointWallet, Long> {

    Optional<PointWallet> findByMemberId(Long memberId);
}
