package com.tutorial.transaction.transaction;

import com.tutorial.shared.wallet.events.TransferDtoEvent;
import com.tutorial.shared.wallet.events.TransferResultDtoEvent;
import com.tutorial.sharedmodule.infra.KafkaTopics;
import com.tutorial.sharedmodule.infra.avro.AvroPayloadSerializer;
import com.tutorial.sharedmodule.infra.outbox.OutBox;
import com.tutorial.sharedmodule.infra.outbox.OutBoxRepository;
import com.tutorial.transaction.transaction.ledger.LedgerService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private final AvroPayloadSerializer avroPayloadSerializer;
  private final OutBoxRepository outBoxRepository;
  private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

  public TransactionService(
      TransactionRepository transactionRepository,
      LedgerService ledgerService,
      KafkaTemplate<String, TransferDtoEvent> kafkaTemplate,
      TransferEventMapper transferEventMapper,
      AvroPayloadSerializer avroPayloadSerializer,
      OutBoxRepository outBoxRepository) {
    this.transactionRepository = transactionRepository;
    this.ledgerService = ledgerService;
    this.kafkaTemplate = kafkaTemplate;
    this.transferEventMapper = transferEventMapper;
    this.avroPayloadSerializer = avroPayloadSerializer;
    this.outBoxRepository = outBoxRepository;
  }

  @Transactional
  public void transfer(TransferDto transferDto, Long userId) {
    createTransfer(
        transferDto,
        userId,
        transferDto.transactionTransferType() == TransactionTransferType.WITHDRAW
            ? TransactionType.WITHDRAW
            : TransactionType.DEPOSIT);
  }

  private void createTransfer(TransferDto transferDto, Long userId, TransactionType transferType) {
    log.info(
        "Transfer started transferType={} idempotencyKey={} userId={}",
        transferType,
        transferDto.idempotencyKey(),
        userId);
    validateIdempotency(transferDto.idempotencyKey());
    Transaction transaction =
        new Transaction(
            userId, transferType, TransactionStatus.PENDING, transferDto.idempotencyKey(), null);
    transactionRepository.save(transaction);
    TransferDtoEvent event = transferEventMapper.toEvent(transferDto, userId);
    saveOutBox(transferDto.idempotencyKey().toString(), userId, event);
    log.info("Transfer event stored in outbox idempotencyKey={}", transferDto.idempotencyKey());
  }

  private void validateIdempotency(UUID idempotencyKey) {

    if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
      log.info("Duplicate transaction detected idempotencyKey={}", idempotencyKey);
      throw new TransactionAlreadyProccesedException(
          "Transaction already processed: " + idempotencyKey);
    }
  }

  private Transaction getTransactionByIdempotencyKey(UUID idempotencyKey) {
    return transactionRepository
        .findByIdempotencyKey(idempotencyKey)
        .orElseThrow(
            () -> {
              log.warn("Transaction not found idempotencyKey={}", idempotencyKey);
              return new EntityNotFoundException(
                  "transaction with uuid " + idempotencyKey + " not found");
            });
  }

  public Page<Transaction> getUserTransactions(Long user, Pageable pageable) {
    Pageable newPageable =
        PageRequest.of(
            pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().descending());
    return transactionRepository.findAllByUserId(user, newPageable);
  }

  @Transactional
  public void processWalletTransferEvent(TransferResultDtoEvent event) {
    log.info(
        "Transaction With Idempotency Key ={} Going To Be Processed", event.getIdempotencyKey());
    Transaction transaction =
        getTransactionByIdempotencyKey(UUID.fromString(event.getIdempotencyKey()));

    if (transaction.getStatus() != TransactionStatus.PENDING) {
      log.info(
          "Ignoring transfer result because transaction is not pending "
              + "idempotencyKey={} expectedStatus={} actualStatus={}",
          event.getIdempotencyKey(),
          TransactionStatus.PENDING,
          transaction.getStatus());
      return;
    }

    switch (event.getStatus()) {
      case SUCCESS -> completeTransaction(event, transaction);
      case FAILED -> failTransaction(event, transaction);
    }
  }

  private void completeTransaction(TransferResultDtoEvent event, Transaction transaction) {
    ledgerService.applyTransfer(
        transaction,
        event.getTransferType(),
        event.getUserWalletId(),
        event.getSystemWalletId(),
        new BigDecimal(event.getAmount()));

    transaction.setStatus(TransactionStatus.COMPLETED);
    log.info(
            "Transaction processed successfully idempotencyKey={}",
            event.getIdempotencyKey()
    );
  }

  private void failTransaction(TransferResultDtoEvent event, Transaction transaction) {
    transaction.setFailedReason(event.getFailedReason());
    transaction.setStatus(TransactionStatus.FAILED);
    log.info(
            "Transaction processed as failed idempotencyKey={} failedReason={}",
            event.getIdempotencyKey(),
            event.getFailedReason()
    );
  }

  private void saveOutBox(String idempotencyKey, Long userId, TransferDtoEvent event) {
    OutBox outBox = new OutBox();
    outBox.setAggregateType("transaction");
    outBox.setAggregateId(idempotencyKey);
    outBox.setEventType("transaction_transfer");
    outBox.setTopic(KafkaTopics.WALLET_TRANSFER);
    outBox.setPayload(avroPayloadSerializer.serialize(KafkaTopics.WALLET_TRANSFER, event));
    outBox.setPartitionKey(userId.toString());
    outBoxRepository.save(outBox);
  }
}
