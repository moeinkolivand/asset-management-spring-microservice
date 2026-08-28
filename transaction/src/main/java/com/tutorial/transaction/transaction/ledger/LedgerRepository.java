package com.tutorial.transaction.transaction.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
  boolean existsByTransactionIdAndWalletIdAndLedgerEntryDirectionEnum(
      Long transactionId, Long walletId, LedgerEntryDirectionEnum direction);

  Optional<LedgerEntry> findByTransactionIdAndWalletIdAndLedgerEntryDirectionEnum(
      Long id, Long walletId, LedgerEntryDirectionEnum direction);
}
