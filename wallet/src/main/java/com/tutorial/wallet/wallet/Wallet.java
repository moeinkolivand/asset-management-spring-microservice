package com.tutorial.wallet.wallet;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "wallets",
        indexes = {
                @Index(name = "idx_wallet_name", columnList = "name"),
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_currency", columnNames = {"user_id", "currency_id"})
        }
)
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "currency_id", nullable = false)
    private Long currencyId;

    @ColumnDefault("'ACTIVE'")
    @Enumerated(EnumType.STRING)
    private WalletStatus walletStatus;


    public Wallet() {
    }

    public Wallet(String name, BigDecimal balance, Long userId, Long currencyId) {
        this.name = name;
        this.balance = balance;
        this.userId = userId;
        this.currencyId = currencyId;
        this.createdAt = Instant.now();
    }

    public Wallet(String name, BigDecimal balance) {
        this.name = name;
        this.balance = balance;
        this.createdAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public WalletStatus getWalletStatus() {
        return walletStatus;
    }


    public Long getCurrencyId() {
        return currencyId;
    }


    public void setWalletStatus(WalletStatus walletStatus) {
        this.walletStatus = walletStatus;
    }
}
