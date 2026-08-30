package com.tutorial.wallet.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemAccountRepository extends JpaRepository<SystemAccount, Long> {
    Optional<SystemAccount> findByUserId(Long userId);

    Optional<SystemAccount> findFirstByOrderByIdAsc();
}
