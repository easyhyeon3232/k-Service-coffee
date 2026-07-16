package com.example.coffee.domin.point.repository;

import com.example.coffee.domin.point.entity.PointWallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PointWalletRepository extends JpaRepository<PointWallet, Long> {

    Optional<PointWallet> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pointWallet from PointWallet pointWallet join fetch pointWallet.member member "
            + "where member.id = :memberId")
    Optional<PointWallet> findByMemberIdForUpdate(@Param("memberId") Long memberId);
}
