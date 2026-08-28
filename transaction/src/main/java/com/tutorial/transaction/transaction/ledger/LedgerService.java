package com.tutorial.transaction.transaction.ledger;

import java.math.BigDecimal;

import com.tutorial.shared.common.avro.TransferTypeEnumEvent;
import com.tutorial.transaction.transaction.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

  private final LedgerRepository ledgerRepository;

  public LedgerService(LedgerRepository ledgerRepository) {
    this.ledgerRepository = ledgerRepository;
  }

  @Transactional
  public void applyTransfer(
      Transaction transaction,
      TransferTypeEnumEvent transferType,
      Long userWalletId,
      Long systemWalletId,
      BigDecimal amount) {

    validateAmount(amount);

    switch (transferType) {
      case DEPOSIT -> applyDeposit(transaction, userWalletId, systemWalletId, amount);

      case WITHDRAW -> applyWithdraw(transaction, userWalletId, systemWalletId, amount);
    }
  }

  private void applyDeposit(
      Transaction transaction, Long userWalletId, Long systemWalletId, BigDecimal amount) {

    createDebit(transaction, systemWalletId, amount);

    createCredit(transaction, userWalletId, amount);
  }

  private void applyWithdraw(
      Transaction transaction, Long userWalletId, Long systemWalletId, BigDecimal amount) {

    createDebit(transaction, userWalletId, amount);

    createCredit(transaction, systemWalletId, amount);
  }

  public LedgerEntry createDebit(Transaction transaction, Long walletId, BigDecimal amount) {

    return createEntry(transaction, walletId, LedgerEntryDirectionEnum.DEBIT, amount);
  }

  public LedgerEntry createCredit(Transaction transaction, Long walletId, BigDecimal amount) {

    return createEntry(transaction, walletId, LedgerEntryDirectionEnum.CREDIT, amount);
  }

  private LedgerEntry createEntry(
      Transaction transaction,
      Long walletId,
      LedgerEntryDirectionEnum direction,
      BigDecimal amount) {

    validateAmount(amount);

    boolean exists =
        ledgerRepository.existsByTransactionIdAndWalletIdAndLedgerEntryDirectionEnum(
            transaction.getId(), walletId, direction);

    //TODO  Wright A Exception For Throw
    if (exists) {
      return ledgerRepository
          .findByTransactionIdAndWalletIdAndLedgerEntryDirectionEnum(
              transaction.getId(), walletId, direction)
          .orElseThrow();
    }

    return ledgerRepository.save(new LedgerEntry(transaction, walletId, direction, amount));
  }

  private void validateAmount(BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) {
      throw new IllegalArgumentException("Transfer amount must be greater than zero");
    }
  }
}
