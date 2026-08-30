package com.tutorial.wallet.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "system_accounts", uniqueConstraints = @UniqueConstraint(
        name = "uq_system_account_user", columnNames = "user_id"))
public class SystemAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    protected SystemAccount() {
    }

    public SystemAccount(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
