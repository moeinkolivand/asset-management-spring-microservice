package com.tutorial.transaction.transaction;

import com.tutorial.shared.wallet.events.WithdrawDtoEvent;
import com.tutorial.transaction.transaction.ledger.LedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final LedgerService ledgerService;
  private final TransactionTemplate transactionTemplate;
  private final KafkaTemplate<String, WithdrawDtoEvent> kafkaTemplate;

  @Autowired
  public TransactionService(
      TransactionRepository transactionRepository,
      LedgerService ledgerService,
      TransactionTemplate transactionTemplate,
      KafkaTemplate<String, WithdrawDtoEvent> kafkaTemplate) {
    this.transactionRepository = transactionRepository;
    this.ledgerService = ledgerService;
    this.transactionTemplate = transactionTemplate;
    this.kafkaTemplate = kafkaTemplate;
  }

  public void withdraw(WithdrawDto withdrawDto, Long userId) {
    transactionRepository
        .findByIdempotencyKey(withdrawDto.idempotencyKey())
        .ifPresent(
            existing -> {
              throw new TransactionAlreadyProccesedException("Already processed");
            });
    kafkaTemplate.send(
        "wallet-withdraw",
        WithdrawDtoEvent.newBuilder()
            .setAmount(withdrawDto.amount().toString())
            .setIdempotencyKey(withdrawDto.idempotencyKey().toString())
            .setCurrencyId(withdrawDto.currencyId())
            .setUserId(userId)
            .build());
  }

  public void deposit(DepositDto depositDto, Long user) {
    transactionRepository
        .findByIdempotencyKey(depositDto.idempotencyKey())
        .ifPresent(
            existing -> {
              throw new TransactionAlreadyProccesedException("Already processed");
            });
    //        WithdrawalWallets withdrawalWallets = resolveAndValidate(depositDto.currencyName(),
    // user);
    //        transactionTemplate.execute(status -> {
    ////            Wallet userWallet =
    // walletApi.findByIdForUpdate(withdrawalWallets.userWallet.getId()).orElseThrow(() -> new
    // ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    ////            Wallet systemWallet =
    // walletApi.findByIdForUpdate(withdrawalWallets.systemWallet.getId()).orElseThrow(() -> new
    // ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    ////            if (isWalletBalanceGoesToNegative(systemWallet, depositDto.amount())) {
    ////                throw new InsufficientWalletBalanceException("Insufficient Balance");
    ////            }
    //            Transaction transaction = transactionRepository.save(new Transaction(
    //                    user, TransactionType.DEPOSIT, TransactionStatus.COMPLETED,
    // depositDto.idempotencyKey()
    //            ));
    //            ledgerService.createDebit(transaction, systemWallet, depositDto.amount());
    //            ledgerService.createCredit(transaction, userWallet,  depositDto.amount());
    //            return null;
    //        });

  }

  public Page<Transaction> getUserTransactions(Long user, Pageable pageable) {
    Pageable newPageable =
        PageRequest.of(
            pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().descending());
    return transactionRepository.findAllByUser(user, newPageable);
  }
}
