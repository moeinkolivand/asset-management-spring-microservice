package com.tutorial.transaction.transaction;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long userId;

    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "failed_reason")
    private String failedReason;

    protected Transaction() {
    }

    public Transaction(
            Long userId,
            TransactionType transactionType,
            TransactionStatus status,
            UUID idempotencyKey,
            String failedReason
    ) {
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("idempotencyKey must be supplied by the client");
        }
        this.userId = userId;
        this.transactionType = transactionType;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
        this.failedReason = failedReason;
    }

    public Long getId() {
        return id;
    }

    public Long getUser() {
        return userId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getFailedReason() {
        return failedReason;
    }
}