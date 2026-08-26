package com.tutorial.transaction.transaction.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
}
