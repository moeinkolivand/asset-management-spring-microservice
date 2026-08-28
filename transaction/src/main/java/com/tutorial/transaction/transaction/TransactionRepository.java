package com.tutorial.transaction.transaction;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdempotencyKey(@NotBlank(message = "idempotencyKey is required") UUID uuid);

    Page<Transaction> findAllByUserId(Long userId, Pageable pageable);
}
