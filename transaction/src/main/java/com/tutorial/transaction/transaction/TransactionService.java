package com.tutorial.transaction.transaction;

import com.tutorial.shared.wallet.events.TransferDtoEvent;
import com.tutorial.shared.wallet.events.WithdrawResultDtoEvent;
import com.tutorial.sharedmodule.infra.KafkaTopics;
import com.tutorial.transaction.transaction.ledger.LedgerService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final LedgerService ledgerService;
  private final KafkaTemplate<String, TransferDtoEvent> kafkaTemplate;
  private final TransferEventMapper transferEventMapper;

  public TransactionService(
      TransactionRepository transactionRepository,
      LedgerService ledgerService,
      KafkaTemplate<String, TransferDtoEvent> kafkaTemplate,
      TransferEventMapper transferEventMapper) {
    this.transactionRepository = transactionRepository;
    this.ledgerService = ledgerService;
    this.kafkaTemplate = kafkaTemplate;
    this.transferEventMapper = transferEventMapper;
  }

  public void transfer(TransferDto transferDto, Long userId) {
    createTransfer(
        transferDto,
        userId,
        transferDto.transactionTransferType() == TransactionTransferType.WITHDRAW
            ? TransactionType.WITHDRAW
            : TransactionType.DEPOSIT);
  }

  private void createTransfer(TransferDto transferDto, Long userId, TransactionType transferType) {
    validateIdempotency(transferDto.idempotencyKey());
    Transaction transaction =
        new Transaction(
            userId, transferType, TransactionStatus.PENDING, transferDto.idempotencyKey(), null);
    transactionRepository.save(transaction);
    TransferDtoEvent event = transferEventMapper.toEvent(transferDto, userId);

    kafkaTemplate.send(KafkaTopics.WALLET_TRANSFER, transferDto.idempotencyKey().toString(), event);
  }

  private void validateIdempotency(UUID idempotencyKey) {

    if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
      throw new TransactionAlreadyProccesedException(
          "Transaction already processed: " + idempotencyKey);
    }
  }

  private Transaction getTransactionByIdempotencyKey(UUID idempotencyKey) {
    return transactionRepository
        .findByIdempotencyKey(idempotencyKey)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "transaction with uuid " + idempotencyKey + " not found"));
  }

  public Page<Transaction> getUserTransactions(Long user, Pageable pageable) {
    Pageable newPageable =
        PageRequest.of(
            pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().descending());
    return transactionRepository.findAllByUserId(user, newPageable);
  }

  @Transactional
  public void processWalletTransferEvent(WithdrawResultDtoEvent event) {

    Transaction transaction =
        getTransactionByIdempotencyKey(UUID.fromString(event.getIdempotencyKey()));

    if (transaction.getStatus() != TransactionStatus.PENDING) {
      return;
    }

    switch (event.getStatus()) {
      case SUCCESS -> completeTransaction(event, transaction);
      case FAILED -> failTransaction(event, transaction);
    }
  }

  private void completeTransaction(WithdrawResultDtoEvent event, Transaction transaction) {
    ledgerService.applyTransfer(
        transaction,
        event.getTransferType(),
        event.getUserWalletId(),
        event.getSystemWalletId(),
        new BigDecimal(event.getAmount()));

    transaction.setStatus(TransactionStatus.COMPLETED);
    System.out.println("event with uuid: " + event.getIdempotencyKey() + "process successed");
  }

  private void failTransaction(WithdrawResultDtoEvent event, Transaction transaction) {
    transaction.setFailedReason(event.getFailedReason());
    transaction.setStatus(TransactionStatus.FAILED);
    System.out.println("event with uuid: " + event.getIdempotencyKey() + "process successed");
  }
}
