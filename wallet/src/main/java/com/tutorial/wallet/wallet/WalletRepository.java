package com.tutorial.wallet.wallet;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    boolean existsByUserIdAndCurrencyId(Long userId, Long currencyId);

    List<Wallet> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);

    Optional<Wallet> findByUserIdAndCurrencyId(Long userId, Long currencyId);

    Optional<Wallet> findByCurrencyIdAndUserId(Long currencyId, Long user);
}
