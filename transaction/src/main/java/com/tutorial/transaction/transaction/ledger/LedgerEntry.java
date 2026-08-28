package com.tutorial.transaction.transaction.ledger;

import com.tutorial.transaction.transaction.Transaction;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "ledger_entries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ledger_transaction_wallet_direction",
                        columnNames = {
                                "transaction_id",
                                "wallet_id",
                                "ledger_entry_direction_enum"
                        }
                )
        }
)
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryDirectionEnum ledgerEntryDirectionEnum;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public LedgerEntry() {}

    public LedgerEntry(Transaction transaction, Long wallet,
                       LedgerEntryDirectionEnum ledgerEntryDirectionEnum, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Ledger entry amount must be positive; direction encodes sign");
        }
        this.transaction = transaction;
        this.walletId = wallet;
        this.ledgerEntryDirectionEnum = ledgerEntryDirectionEnum;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Transaction getTransaction() { return transaction; }
    public Long getWalletId() { return walletId; }
    public LedgerEntryDirectionEnum getLedgerEntryDirectionEnum() { return ledgerEntryDirectionEnum; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}